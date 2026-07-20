// supabase/functions/notify-checkin/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
const FCM_SERVER_KEY = Deno.env.get("FCM_SERVER_KEY")!

serve(async (req) => {
    const { record } = await req.json()  // payload del webhook
    const userId = record.user_id

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY)

    // Obtener username del que hizo check-in
    const { data: profile } = await supabase
        .from("user_profiles")
        .select("username, avatar_emoji")
        .eq("id", userId)
        .single()

    // Obtener tokens de amigos que no han entrenado hoy
    const { data: targets } = await supabase
        .rpc("get_friend_tokens_not_checked_in", { p_user_id: userId })

    if (!targets || targets.length === 0) {
        return new Response("No targets", { status: 200 })
    }

    // Enviar FCM a cada amigo
    const notifications = targets.map((target: any) =>
        fetch("https://fcm.googleapis.com/fcm/send", {
            method: "POST",
            headers: {
                "Authorization": `key=${FCM_SERVER_KEY}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                to: target.fcm_token,
                notification: {
                    title: "💪 ¡Tu amigo ya entrenó!",
                    body: `${profile.avatar_emoji} ${profile.username} acaba de hacer check-in. ¡No te quedes atrás!`
                },
                data: { type: "friend_checkin" }
            })
        })
    )

    await Promise.all(notifications)
    return new Response("OK", { status: 200 })
})