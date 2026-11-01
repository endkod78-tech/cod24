package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ErrorCodesScreen
import com.example.ui.screens.NewOrderScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProblemsScreen
import com.example.ui.screens.TechniciansScreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.KodyarTheme
import com.example.ui.viewmodel.KodyarViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KodyarViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            KodyarTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var currentTab by remember { mutableStateOf("technicians") }

                    Scaffold(
                        topBar = {
                            if (currentTab != "new_order") {
                                CenterAlignedTopAppBar(
                                    title = {
                                        Text(
                                            text = "کدیار۲۴ • نسخه مشتری",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (currentTab != "new_order") {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 6.dp
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == "technicians",
                                        onClick = { currentTab = "technicians" },
                                        icon = {
                                            Icon(
                                                Icons.Default.Engineering,
                                                contentDescription = "تکنسین‌ها"
                                            )
                                        },
                                        label = { Text("تکنسین‌ها") },
                                        modifier = Modifier.testTag("nav_technicians"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandBlue,
                                            selectedTextColor = BrandBlue
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentTab == "orders",
                                        onClick = { currentTab = "orders" },
                                        icon = {
                                            Icon(
                                                Icons.Default.ListAlt,
                                                contentDescription = "سفارش‌های من"
                                            )
                                        },
                                        label = { Text("سفارش‌های من") },
                                        modifier = Modifier.testTag("nav_orders"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandBlue,
                                            selectedTextColor = BrandBlue
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentTab == "errors",
                                        onClick = { currentTab = "errors" },
                                        icon = {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = "کدهای خطا"
                                            )
                                        },
                                        label = { Text("کدهای خطا") },
                                        modifier = Modifier.testTag("nav_errors"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandBlue,
                                            selectedTextColor = BrandBlue
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentTab == "problems",
                                        onClick = { currentTab = "problems" },
                                        icon = {
                                            Icon(
                                                Icons.Default.Build,
                                                contentDescription = "خدمات و قطعات"
                                            )
                                        },
                                        label = { Text("خدمات و قطعات") },
                                        modifier = Modifier.testTag("nav_problems"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandBlue,
                                            selectedTextColor = BrandBlue
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                "technicians" -> TechniciansScreen(
                                    viewModel = viewModel,
                                    onSelectTechnician = { tech ->
                                        currentTab = "new_order"
                                    }
                                )
                                "orders" -> OrdersScreen(
                                    viewModel = viewModel,
                                    onNavigateToNewOrder = { currentTab = "new_order" }
                                )
                                "new_order" -> NewOrderScreen(
                                    viewModel = viewModel,
                                    onBack = { currentTab = "technicians" }
                                )
                                "errors" -> ErrorCodesScreen(
                                    viewModel = viewModel
                                )
                                "problems" -> ProblemsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
