import re

with open("app/src/main/java/com/example/RentVsBuyTab.kt", "r") as f:
    lines = f.readlines()

# find downPaymentInput section
start_idx = -1
for i, l in enumerate(lines):
    if "value = downPaymentInput," in l:
        start_idx = i - 1
        break

end_idx = -1
for i, l in enumerate(lines):
    if "fun RentVsBuyProjectionsList(" in l:
        end_idx = i
        break

fixed_text = """                OutlinedTextField(
                    value = downPaymentInput,
                    onValueChange = { onDownPaymentChange(coerceInputString(it, 100000000.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Enganche ($cur)"
                        LanguageCode.FR -> "Apport ($cur)"
                        LanguageCode.DE -> "Anzahlung ($cur)"
                        LanguageCode.HI -> "डाउन पेमेंट ($cur)"
                        LanguageCode.TA -> "முன்பணம் ($cur)"
                        else -> "Down Payment ($cur)"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_down_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { onHelpClick("LTV") }) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Down Payment Help")
                        }
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = interestRateInput,
                    onValueChange = { onInterestRateChange(coerceInputString(it, 30.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Tasa %"
                        LanguageCode.FR -> "Taux %"
                        LanguageCode.DE -> "Zins %"
                        LanguageCode.HI -> "ब्याज %"
                        LanguageCode.TA -> "வட்டி %"
                        else -> "Interest Rate %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f).testTag("buy_rate_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = loanTermInput,
                    onValueChange = { onLoanTermChange(coerceInputString(it, 50.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Plazo (Años)"
                        LanguageCode.FR -> "Durée (Ans)"
                        LanguageCode.DE -> "Laufzeit (Jahre)"
                        LanguageCode.HI -> "अवधि (वर्ष)"
                        LanguageCode.TA -> "காலம் (ஆண்டுகள்)"
                        else -> "Years"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_term_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = taxRateInput,
                    onValueChange = { onTaxRateChange(coerceInputString(it, 10.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Impuesto Propiedad %"
                        LanguageCode.FR -> "Taxe Foncière %"
                        LanguageCode.DE -> "Grundsteuer %"
                        LanguageCode.HI -> "संपत्ति कर %"
                        LanguageCode.TA -> "சொத்து வரி %"
                        else -> "Property Tax %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f).testTag("buy_tax_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = homeInsInput,
                    onValueChange = { onHomeInsChange(coerceInputString(it, 50000.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Seguro ($cur/año)"
                        LanguageCode.FR -> "Assurance ($cur/an)"
                        LanguageCode.DE -> "Versicherung ($cur/J)"
                        LanguageCode.HI -> "बीमा ($cur/वर्ष)"
                        LanguageCode.TA -> "காப்பீடு ($cur/ஆண்டு)"
                        else -> "Home Ins. ($cur/yr)"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_ins_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = registrationTaxInput,
                    onValueChange = { onRegistrationTaxChange(coerceInputString(it, 20.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Impuesto de Registro %"
                        LanguageCode.FR -> "Droits de Mutation %"
                        LanguageCode.DE -> "Grunderwerbsteuer %"
                        LanguageCode.HI -> "पंजीकरण कर %"
                        LanguageCode.TA -> "பதிவு வரி %"
                        else -> "Closing Cost %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f).testTag("buy_reg_tax_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = maintenanceInput,
                    onValueChange = { onMaintenanceChange(coerceInputString(it, 10.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Mantenimiento %"
                        LanguageCode.FR -> "Entretien %"
                        LanguageCode.DE -> "Instandhaltung %"
                        LanguageCode.HI -> "रखरखाव %"
                        LanguageCode.TA -> "பராமரிப்பு %"
                        else -> "Maintenance %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_maint_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = appreciationInput,
                    onValueChange = { onAppreciationChange(coerceInputString(it, 20.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Apreciación %"
                        LanguageCode.FR -> "Appréciation %"
                        LanguageCode.DE -> "Wertsteigerung %"
                        LanguageCode.HI -> "वार्षिक प्रशंसा %"
                        LanguageCode.TA -> "மதிப்பு உயர்வு %"
                        else -> "Appreciation %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_appr_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { onHelpClick("ROI") }) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "ROI Help")
                        }
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RentVsBuyDashboard(
    lang: LanguageCode,
    cur: String,
    projectionList: List<YearlyComparisonRow>,
    breakEvenYear: Int?,
    plannedYears: Int,
    showExtendedProjection: Boolean,
    onToggleExtendedProjection: (Boolean) -> Unit,
    onHelpClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when(lang) {
                        LanguageCode.ES -> if (showExtendedProjection) "Cuadro de mando (Plazo completo 30 Años)" else "Cuadro de mando ($plannedYears Años)"
                        LanguageCode.FR -> if (showExtendedProjection) "Tableau (Global 30 Ans)" else "Tableau ($plannedYears Ans)"
                        LanguageCode.DE -> if (showExtendedProjection) "Finanzübersicht (Komplett 30 Jahre)" else "Finanzübersicht ($plannedYears Jahre)"
                        LanguageCode.HI -> if (showExtendedProjection) "सारांश (पूर्ण 30 वर्ष)" else "सारांश ($plannedYears वर्ष)"
                        LanguageCode.TA -> if (showExtendedProjection) "சுருக்கம் (முழு 30 ஆண்டுகள்)" else "சுருக்கம் ($plannedYears ஆண்டுகள்)"
                        else -> if (showExtendedProjection) "Financial Overview (30-Year Complete)" else "Financial Overview (Year $plannedYears)"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "30-Yr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = showExtendedProjection,
                        onCheckedChange = onToggleExtendedProjection,
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }

            val targetRow = if (showExtendedProjection) projectionList.last() else projectionList.firstOrNull { it.year == plannedYears } ?: projectionList.last()
            val targetYearLabel = if (showExtendedProjection) "30" else plannedYears.toString()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Rent Cumulative (Rentabilidad)"
                            LanguageCode.FR -> "Location Cumulé"
                            LanguageCode.DE -> "Miete Kumuliert"
                            LanguageCode.HI -> "किराया संचयी व्यय"
                            LanguageCode.TA -> "வாடகை மொத்த செலவு"
                            else -> "Rent Total Spend"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$cur ${String.format("%,.0f2", targetRow.rentCumulativeSpend).replace(".0f2", "")}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when(lang) {
                                LanguageCode.ES -> "Buy Net Cost (Costo Neto)"
                                LanguageCode.FR -> "Achat Coût Net"
                                LanguageCode.DE -> "Kauf Netto-Zahlung"
                                LanguageCode.HI -> "खरीदने की शुद्ध लागत"
                                LanguageCode.TA -> "வாங்குதலின் நிகர மதிப்பு"
                                else -> "Buy Net Cost"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { onHelpClick("BrokerFee") }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Broker Fee Info", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "$cur ${String.format("%,.0f2", targetRow.buyNetCost).replace(".0f2", "")}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (targetRow.buyNetCost < targetRow.rentCumulativeSpend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when(lang) {
                        LanguageCode.ES -> "Detalle de los Bienes Adquiridos"
                        LanguageCode.FR -> "Capitalisation de l'actif immobilier"
                        LanguageCode.DE -> "Investition- & Vermögensbildung"
                        LanguageCode.HI -> "निवेश मूल्य सृजन"
                        LanguageCode.TA -> "வீட்டின் சொத்து மதிப்பு விவரம்"
                        else -> "Home Value Assets Created"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Valor Estimado (Año $targetYearLabel)"
                            LanguageCode.FR -> "Valorisation estimée (Ans $targetYearLabel)"
                            LanguageCode.DE -> "Geschätzter Wert (Jahr $targetYearLabel)"
                            LanguageCode.HI -> "अनुमानित मूल्य (वर्ष $targetYearLabel)"
                            LanguageCode.TA -> "மதிப்பு ($targetYearLabel-ஆம் ஆண்டு)"
                            else -> "Est. Property Value (Year $targetYearLabel)"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "$cur ${String.format("%,.0f2", targetRow.buyHomeValue).replace(".0f2", "")}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Capital acumulado libre de deudas"
                            LanguageCode.FR -> "Capital acquis (Libre de dette)"
                            LanguageCode.DE -> "Gebildetes Eigenkapital (Schuldenfrei)"
                            LanguageCode.HI -> "निर्मित संचित इक्विटी"
                            LanguageCode.TA -> "உங்களது சொந்த பங்கு மதிப்பு"
                            else -> "Equity Acquired (Debt-Free)"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "$cur ${String.format("%,.0f2", targetRow.buyEquity).replace(".0f2", "")}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

"""

new_lines = lines[:start_idx] + [fixed_text] + lines[end_idx:]

with open("app/src/main/java/com/example/RentVsBuyTab.kt", "w") as f:
    f.writelines(new_lines)
