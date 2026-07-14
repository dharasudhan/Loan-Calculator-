with open("gradle/libs.versions.toml", "r") as f:
    text = f.read()
text = text.replace('userMessagingPlatform = "3.1.0"\n\n[libraries]\nuser-messaging-platform = { group = "com.google.android.ump", name = "user-messaging-platform", version.ref = "userMessagingPlatform" }', '')
text = text.replace('[plugins]', 'user-messaging-platform = { group = "com.google.android.ump", name = "user-messaging-platform", version = "3.1.0" }\n\n[plugins]')
with open("gradle/libs.versions.toml", "w") as f:
    f.write(text)
