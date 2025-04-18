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
package at.crowdware.nocodelibmobile.ui.widgets

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.crowdware.nocodelibmobile.BaseComposeActivity
import at.crowdware.nocodelibmobile.R
import at.crowdware.nocodelibmobile.ui.theme.OnPrimary
import at.crowdware.nocodelibmobile.ui.theme.Primary
import at.crowdware.nocodelibmobile.utils.NavigationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NavigationItem( val id: String, val url: String = "", val icon: ImageVector? = null, val text: String = "", val index: Int = 0)


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DrawerSheet(
    drawerState: DrawerState,
    items: List<NavigationItem>,
    selectedItem: MutableState<String>,
    mainActivity: BaseComposeActivity
) {
    val scope = rememberCoroutineScope()
    val listState = remember { mutableStateOf(items) }

    if (drawerState.offset.value > -540f) {
        ModalDrawerSheet(
            modifier = Modifier
                .width((LocalConfiguration.current.screenWidthDp * 0.75).dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Primary)
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.icon),
                        contentDescription = stringResource(id = R.string.icon),
                        modifier = Modifier.weight(1f),
                    )
                    Text("© 2025 CrowdWare", color = OnPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("http://crowdware.info", color = OnPrimary)
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn() {
                items(items.size) { index ->
                    if (items[index].id == "divider") {
                        Divider()
                    } else if (items[index].text.isNotEmpty()) {

                        NavigationDrawerItem(
                            icon = {
                                if(items[index].icon != null) {
                                    Icon(
                                        items[index].icon!!,
                                        contentDescription = items[index].text
                                    )
                                }
                            },
                            label = { Text(items[index].text) },
                            selected = items[index].text == selectedItem.value,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    selectedItem.value = items[index].text
                                    if(items[index].url.isNotEmpty()) {
                                        withContext(Dispatchers.IO) {
                                            mainActivity.contentLoader.switchApp(items[index].url)
                                        }
                                    }
                                    NavigationManager.navigate(items[index].id)
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    }
}

