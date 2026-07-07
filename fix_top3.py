with open("app/src/main/java/com/example/RentVsBuyTab.kt", "r") as f:
    text = f.read()

text = text.replace('import androidx.compose.ui.draw.scalepackage com.example', 'package com.example\nimport androidx.compose.ui.draw.scale\n')
text = text.replace('import com.example.LoanCalculatorViewModelimport', 'import com.example.LoanCalculatorViewModel\nimport')
text = text.replace('import com.example.LanguageCodeimport', 'import com.example.LanguageCode\nimport')
text = text.replace('import com.example.currencySymbolimport', 'import com.example.currencySymbol\nimport')
text = text.replace('scale\nimport com.example.LoanCalculatorViewModel', 'scale\nimport com.example.LoanCalculatorViewModel\n')
text = text.replace('import androidx.compose.foundation.backgroundimport', 'import androidx.compose.foundation.background\nimport')

# wait, maybe it's easier to just find the `package com.example` and everything before it, and replace.
# Let's see what the top actually is.
