//package com.northsignalstudio.myram.ui.screens
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.northsignalstudio.myram.ui.NotesViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun FoldersScreen(vm: NotesViewModel = viewModel()) {
//    val folders by vm.folders.collectAsState()
//
//    Scaffold(
//        floatingActionButton = {
//            FloatingActionButton(onClick = { vm.createFolder("New Folder") }) {
//                Text("+")
//            }
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .padding(padding)
//                .fillMaxSize()
//                .padding(16.dp)
//        ) {
//            Text("Folders", style = MaterialTheme.typography.headlineSmall)
//            Spacer(Modifier.height(8.dp))
//            folders.forEach { folder ->
//                Card(
//                    Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 4.dp)
//                        .clickable { vm.selectFolder(folder) }
//                ) {
//                    Text(
//                        folder.name,
//                        Modifier.padding(16.dp),
//                        style = MaterialTheme.typography.bodyLarge
//                    )
//                }
//            }
//        }
//    }
//}
