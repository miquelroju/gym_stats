import { createClient } from 'jsr:@supabase/supabase-js@2'
import { GoogleAuth } from 'npm:google-auth-library@9'

Deno.serve(async () => {
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
  )

  const { data: feed } = await supabase.rpc('get_checkin_feed')
  const notCheckedIn = feed?.filter((u: any) => !u.checked_in) ?? []
  const checkedIn = feed?.filter((u: any) => u.checked_in) ?? []

  if (notCheckedIn.length === 0 || checkedIn.length === 0) {
    return new Response('Nothing to notify', { status: 200 })
  }

  // 2. Obtener tokens FCM de los usuarios que NO han entrenado
  const { data: tokens } = await supabase
    .from('device_tokens')
    .select('fcm_token, user_id')
    .in('user_id', notCheckedIn.map((u: any) => u.user_id))

  if (!tokens || tokens.length === 0) {
	return new Response('No tokens found', { status: 200 })
  }
  
  // 3. Generar token de acceso OAuth2 dinámico (caduca cada hora)
  const serviceAccount = JSON.parse(Deno.env.get('FIREBASE_SERVICE_ACCOUNT')!)
  const auth = new GoogleAuth({
	  credentials: serviceAccount,
	  scopes: ['https://www.googleapis.com/auth/firebase.messaging']
  })
  const accessToken = await auth.getAccessToken()
  const projectId = serviceAccount.project_id
  const names = checkedIn.map((u: any) => u.username).join(', ')

  // 4. Enviar notificación a cada dispositivo
  for (const t of tokens) {
    await fetch(
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
              title: '💪 ¡Vamos al gimnasio!',
              body: `${names} ya han entrenado hoy. ¿Te animas tú también?`
            }
          }
        })
      }
    )
  }

  return new Response('Notifications sent', { status: 200 })
})