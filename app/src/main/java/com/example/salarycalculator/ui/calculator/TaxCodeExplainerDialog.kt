package com.example.salarycalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Rose60

@Composable
fun TaxCodeExplainerDialog(
    currentTaxCode: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("UK Tax Code Explainer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Active Tax Code: $currentTaxCode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Your tax code tells HMRC how much tax-free income you are entitled to in the current tax year.", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text("Common Tax Code Letters & Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                val codes = listOf(
                    Triple("1257L", "Standard UK Code", "Entitles you to the full £12,570 standard Personal Allowance (numbers multiplied by 10)."),
                    Triple("BR", "Basic Rate (Flat 20%)", "All income from this job is taxed at the 20% basic rate without personal allowance. Common for second jobs."),
                    Triple("0T", "Zero Allowance", "Your personal allowance has been fully used up or your new employer doesn't have your P45 details yet."),
                    Triple("D0", "Higher Rate (Flat 40%)", "All earnings taxed at 40%. Used for high-income second jobs."),
                    Triple("D1", "Additional Rate (Flat 45%)", "All earnings taxed at 45% (above £125,140/yr)."),
                    Triple("M / N", "Marriage Allowance", "M = received 10% (£1,260) from spouse. N = transferred 10% to spouse."),
                    Triple("S Prefix", "Scottish Tax Rates", "Indicates your primary residence is in Scotland and subject to Scottish 6-tier tax bands.")
                )

                codes.forEach { (code, title, desc) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(code, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}
