/****************************************************************************
 * Copyright (C) 2025 CrowdWare
 *
 * This file is part of NoCodeLibMobile.
 *
 *  NoCodeLibMobile is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  NoCodeLibMobile is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with NoCodeLibMobile.  If not, see <http://www.gnu.org/licenses/>.
 *
 ****************************************************************************/
package at.crowdware.nocodelibmobile.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.crowdware.nocodelibmobile.R
import at.crowdware.nocodelibmobile.logic.LocaleManager
import at.crowdware.nocodelibmobile.ui.theme.OnPrimary
import at.crowdware.nocodelibmobile.ui.theme.Primary
import at.crowdware.nocodelibmobile.ui.widgets.DropDownListbox
import at.crowdware.nocodelibmobile.utils.Link
import at.crowdware.nocodelibmobile.ui.widgets.rememberDropDownListboxStateHolder
import at.crowdware.nocodelibmobile.BaseComposeActivity


@Composable
fun Settings() {
    val context = LocalContext.current
    val title = remember { mutableStateOf("") }
    val url = remember { mutableStateOf("") }
    var addButtonEnabled by remember { mutableStateOf(false) }
    var links =
        remember { mutableStateListOf<Link>() } // Verwenden Sie mutableStateListOf für eine reaktive Liste

    if (context is BaseComposeActivity) {
        if (links.isEmpty())
            links.addAll(context.contentLoader.links) // Fügen Sie Links zur Liste hinzu
    }
    //region vars for the DropDownlistbox
    val languages = LocaleManager.getLanguages()
    val index = LocaleManager.getLanguageIndex()
    val currentActivity = LocalContext.current as? BaseComposeActivity
    val onSelectedIndexChanged: (Int) -> Unit = { idx ->
        LocaleManager.setLocale(context, idx)
        currentActivity?.recreate()
    }
    val stateHolderLanguage =
        rememberDropDownListboxStateHolder(languages, index, onSelectedIndexChanged)
    //endregion
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.personal_data), fontWeight = FontWeight.Bold,
                style = TextStyle(fontSize = 18.sp),
                modifier = Modifier.align(Alignment.Start)
            )
            DropDownListbox(
                label = stringResource(R.string.select_preferred_language),
                stateHolder = stateHolderLanguage,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.links), fontWeight = FontWeight.Bold,
                style = TextStyle(fontSize = 18.sp),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(links.size) { index ->
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = links[index].titel,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = links[index].url,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = {
                                if (currentActivity != null) {
                                    currentActivity.contentLoader.removeLink(links[index].titel, links[index].url)
                                    currentActivity?.recreate()
                                }
                                links.removeAt(index)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Link"
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.add_new_link), fontWeight = FontWeight.Bold,
                style = TextStyle(fontSize = 18.sp),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = title.value,
                label = { Text(stringResource(R.string.title)) },
                onValueChange = { nt ->
                    title.value = nt
                    addButtonEnabled = title.value.isNotEmpty() && url.value.isNotEmpty()
                })

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = url.value,
                label = { Text(stringResource(R.string.url)) },
                onValueChange = { nt ->
                    url.value = nt
                    addButtonEnabled = title.value.isNotEmpty() && url.value.isNotEmpty()
                })

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                ),
                onClick = {
                    if (context is BaseComposeActivity) {
                        context.contentLoader.addLink(title.value, url.value)
                        links.add(Link(title.value, url.value)) // Link zur Liste hinzufügen
                    }
                    title.value = ""
                    url.value = ""
                    addButtonEnabled = false
                    currentActivity?.recreate()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = addButtonEnabled
            ) {
                Text(
                    stringResource(R.string.add_link),
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontSize = 18.sp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


@Composable
fun SettingsDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", color = MaterialTheme.colorScheme.onBackground) },
        text = { Text("Do you want to close the book?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    )
}