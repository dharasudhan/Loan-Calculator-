with open("app/src/main/java/com/example/RentVsBuyTab.kt", "r") as f:
    text = f.read()

text = text.replace('import androidx.compose.ui.draw.scale\npackage com.example', 'package com.example\nimport androidx.compose.ui.draw.scale')

with open("app/src/main/java/com/example/RentVsBuyTab.kt", "w") as f:
    f.write(text)
