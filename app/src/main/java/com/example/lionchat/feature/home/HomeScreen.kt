package com.example.lionchat.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController){
    val viewModel = hiltViewModel<HomeViewModel>()
    val channels = viewModel.channels.collectAsState()
    val addChannel = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val searchQuery = remember { mutableStateOf("") }

    val filteredChannels = remember(channels.value, searchQuery.value) {
        if (searchQuery.value.isEmpty()) {
            channels.value // Если поле поиска пустое, отображаем все каналы
        } else {
            channels.value.filter { channel ->
                channel.name.startsWith(searchQuery.value, ignoreCase = true) // Фильтрация по начальным символам
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            Box(modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFDC57))
                .clickable {
                    addChannel.value = true
                }) {
                Text(
                    text = "Add Channel",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Black
                )
            }
        }, containerColor = Color(0xFF432818) // фон
    ){
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            LazyColumn {
                item {
                    Text(
                        text = "Messages" ,
                        color = Color(0xFFFFE7B8),
                        style = TextStyle(fontSize = 20.sp,
                            fontWeight = FontWeight.Black),
                        modifier = Modifier.padding(16.dp))
                }

                item {
                    TextField(
                        value = searchQuery.value,
                        onValueChange = { searchQuery.value = it },
                        placeholder = { Text(text = "Search...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFF432818))
                            .clip(RoundedCornerShape(40.dp)),
                        textStyle = TextStyle(color = Color.Black),
                        colors = TextFieldDefaults.colors().copy(
                            focusedContainerColor = Color(0xFFffe6a7),
                            unfocusedContainerColor = Color(0xFFffe6a7),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = Color.Black // Цвет иконки
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done // Убираем возможность ввода Enter
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {}
                        )
                    )
                }

                items(filteredChannels) {channel ->
                    Column {
                        ChannelItem(
                            channelName = channel.name,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        ) {
                            navController.navigate("chat/${channel.id}&${channel.name}")
                        }
                    }
                }
            }
        }
    }
    if(addChannel.value){
        ModalBottomSheet(onDismissRequest = { addChannel.value = false}, sheetState = sheetState ) {
            AddChannelDialog {
                viewModel.addChannel(it)
                addChannel.value = false
            }
        }
    }
}

@Composable
fun ChannelItem(channelName: String, modifier: Modifier,onClick: () -> Unit) {
    Row (modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 2.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFFffe6a7))
        .clickable {
            onClick()
        },
        verticalAlignment = Alignment.CenterVertically
    ){
        Box(modifier = Modifier
            .padding(8.dp)
            .size(70.dp)
            .clip(CircleShape)
            .background(Color(0xFFffe6a7))
            .shadow(2.dp, CircleShape, ambientColor = Color(0xFFFF4100).copy(alpha = 0.3f))
        ){
            Text(
                text = channelName[0].uppercase(),
                color = Color.Black,
                style = TextStyle(fontSize = 35.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(text = channelName, modifier = Modifier.padding(8.dp), color = Color.Black)
    }
}

@Preview
@Composable
fun PreviewItem(){
    ChannelItem(channelName = "Test Channel", Modifier, {})
}

@Composable
fun AddChannelDialog(onAddChannel: (String) -> Unit) {
    val channelName = remember {
        mutableStateOf("")
    }
    Column (modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "Add Channel", color = Color(0xFF3E3E3E))
        Spacer(modifier = Modifier.padding(8.dp))
        TextField(value = channelName.value, onValueChange = {
            channelName.value = it
        }, label = { Text(text = "Channel Name") }, singleLine = true,
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor = Color(0xFFF1F8E9),
                unfocusedContainerColor = Color(0xFFF1F8E9)
            ))
        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = {onAddChannel(channelName.value) }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Add", color = Color(0xFF3E3E3E))
        }
    }
}