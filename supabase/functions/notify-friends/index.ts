import { createClient } from 'jsr:@supabase/supabase-js@2'
import { GoogleAuth } from 'npm:google-auth-library@9'

Deno.serve(async (req) => {
  // El webhook de Supabase envía el record insertado en el body
  const body = await req.json()
  const record = body?.record
  const userId: string = record?.user_id

  if (!userId) {
    return new Response('Missing user_id in record', { status: 400 })
  }

  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
  )

  // 1. Obtener el perfil del usuario que hizo check-in
  const { data: profile } = await supabase
    .from('user_profiles')
    .select('username, avatar_emoji')
    .eq('id', userId)
    .single()

  if (!profile) {
    return new Response('User profile not found', { status: 404 })
  }

  // 2. Obtener IDs de amigos (en ambas direcciones)
  const { data: friendsA } = await supabase
    .from('friendships')
    .select('friend_id')
    .eq('user_id', userId)

  const { data: friendsB } = await supabase
    .from('friendships')
    .select('user_id')
    .eq('friend_id', userId)

  const friendIds = [
    ...(friendsA ?? []).map((f: any) => f.friend_id),
    ...(friendsB ?? []).map((f: any) => f.user_id)
  ]

  if (friendIds.length === 0) {
    return new Response('No friends found', { status: 200 })
  }

  // 3. Filtrar amigos que NO han entrenado hoy
  const today = new Date().toISOString().split('T')[0]
  const { data: checkedInToday } = await supabase
    .from('daily_checkins')
    .select('user_id')
    .in('user_id', friendIds)
    .eq('date', today)

  const checkedInIds = new Set((checkedInToday ?? []).map((c: any) => c.user_id))
  const targetIds = friendIds.filter((id) => !checkedInIds.has(id))

  if (targetIds.length === 0) {
    return new Response('All friends already checked in', { status: 200 })
  }

  // 4. Obtener tokens FCM de los amigos que NO han entrenado
  const { data: tokens } = await supabase
    .from('device_tokens')
    .select('fcm_token, user_id')
    .in('user_id', targetIds)

  // Filtrar los que tienen notificaciones desactivadas
  const { data: settingsData } = await supabase
    .from('user_settings')
    .select('user_id, notifications_enabled')
    .in('user_id', targetIds)
    .eq('notifications_enabled', true)

  const enabledIds = new Set((settingsData ?? []).map((s: any) => s.user_id))
  const filteredTokens = (tokens ?? []).filter((t: any) => enabledIds.has(t.user_id))

  if (filteredTokens.length === 0) {
    return new Response('No tokens to notify', { status: 200 })
  }

  // 5. Generar token OAuth2 para FCM v1
  const serviceAccount = JSON.parse(Deno.env.get('FIREBASE_SERVICE_ACCOUNT')!)
  const auth = new GoogleAuth({
    credentials: serviceAccount,
    scopes: ['https://www.googleapis.com/auth/firebase.messaging']
  })
  const accessToken = await auth.getAccessToken()
  const projectId = serviceAccount.project_id

  const senderName = `${profile.avatar_emoji ?? '💪'} ${profile.username}`

  // 6. Enviar notificación a cada amigo que aún no ha entrenado
  const sends = filteredTokens.map((t: any) =>
    fetch(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          message: {
            token: t.fcm_token,
            notification: {
              title: '💪 ¡Tu amigo ya entrenó!',
              body: `${senderName} acaba de hacer check-in. ¡No te quedes atrás!`
            },
            data: {
              type: 'friend_checkin',
              sender_id: userId
            }
          }
        })
      }
    )
  )

  await Promise.all(sends)

  return new Response(`Notified ${filteredTokens.length} friends`, { status: 200 })
})
