import { createClient } from 'jsr:@supabase/supabase-js@2'

Deno.serve(async () => {
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
  )

  const { data: feed } = await supabase.rpc('get_checkin_feed')
  const notCheckedIn = feed.filter((u: any) => !u.checked_in)
  const checkedIn = feed.filter((u: any) => u.checked_in)

  if (notCheckedIn.length === 0 || checkedIn.length === 0) {
    return new Response('Nothing to notify', { status: 200 })
  }

  const { data: tokens } = await supabase
    .from('device_tokens')
    .select('fcm_token, user_id')
    .in('user_id', notCheckedIn.map((u: any) => u.user_id))

  const names = checkedIn.map((u: any) => u.username).join(', ')

  for (const t of tokens ?? []) {
    await fetch(
      `https://fcm.googleapis.com/v1/projects/${Deno.env.get('FIREBASE_PROJECT_ID')}/messages:send`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${Deno.env.get('FCM_ACCESS_TOKEN')}`,
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