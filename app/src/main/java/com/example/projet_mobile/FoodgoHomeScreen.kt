
package com.example.projet_mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FoodRed = Color(0xFFF3233D)
private val FoodBackground = Color(0xFFF8F8FA)
private val FoodText = Color(0xFF303038)
private val FoodGray = Color(0xFF85858D)

@Composable
fun FoodgoHomeScreen() {

    var searchText by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("burger") }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    var selectedNav by remember {
        mutableIntStateOf(0)
    }

    var meals by remember {
        mutableStateOf<List<Meal>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var favorites by remember {
        mutableStateOf(setOf<String>())
    }

    // Appel de l'API au lancement et lors d'une recherche
    LaunchedEffect(submittedQuery) {
        loading = true
        error = null

        try {
            val response = withContext(Dispatchers.IO) {
                MealApi.service.searchMeals(submittedQuery)
            }

            meals = response.meals.orEmpty()
        } catch (e: Exception) {
            error = "Impossible de charger les repas."
        } finally {
            loading = false
        }
    }

    val categories = listOf(
        "All", "Beef", "Chicken", "Lamb", "Miscellaneous"
    )

    val filteredMeals = meals.filter { meal ->
        val matchesCategory =
            selectedCategory == "All" ||
                    meal.strCategory.equals(
                        selectedCategory,
                        ignoreCase = true
                    )

        val matchesFavorites =
            selectedNav != 1 || meal.idMeal in favorites

        matchesCategory && matchesFavorites
    }

    Scaffold(
        containerColor = FoodBackground,
        bottomBar = {
            FoodgoBottomBar(
                selected = selectedNav,
                onSelect = { selectedNav = it }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Foodgo",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FoodText
                        )

                        Text(
                            text = "Order your favourite food!",
                            fontSize = 14.sp,
                            color = FoodGray
                        )
                    }

                    // Avatar décoratif
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(
                                1.dp,
                                Color(0xFFE8E8E8),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil",
                            tint = FoodRed,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            // BARRE DE RECHERCHE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        placeholder = {
                            Text("Search", fontSize = 14.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Rechercher"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    IconButton(
                        onClick = {
                            if (searchText.isNotBlank()) {
                                submittedQuery = searchText.trim()
                                selectedCategory = "All"
                                selectedNav = 0
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(FoodRed)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Rechercher",
                            tint = Color.White
                        )
                    }
                }
            }

            // FILTRES
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categories.forEach { category ->
                        val selected = category == selectedCategory

                        Surface(
                            onClick = {
                                selectedCategory = category
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) FoodRed
                            else Color(0xFFF0F0F2),
                            shadowElevation = if (selected) 3.dp else 0.dp
                        ) {
                            Text(
                                text = category,
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical = 12.dp
                                ),
                                color = if (selected) Color.White
                                else FoodGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // TITRE DE LA LISTE
            item {
                Text(
                    text = if (selectedNav == 1) {
                        "My favourites"
                    } else {
                        "Popular burgers"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FoodText
                )
            }

            // CHARGEMENT
            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = FoodRed
                        )
                    }
                }
            }

            // ERREUR
            if (!loading && error != null) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(error.orEmpty(), color = FoodGray)

                        TextButton(
                            onClick = {
                                submittedQuery = submittedQuery
                                    .ifBlank { "burger" }
                            }
                        ) {
                            Text("Réessayer", color = FoodRed)
                        }
                    }
                }
            }

            // CARTES EN GRILLE : 2 PAR LIGNE
            if (!loading && error == null) {
                val rows = filteredMeals.chunked(2)

                if (rows.isEmpty()) {
                    item {
                        Text(
                            text = "Aucun repas trouvé.",
                            color = FoodGray,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                }

                items(rows) { rowMeals ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowMeals.forEach { meal ->
                            MealCard(
                                meal = meal,
                                isFavorite = meal.idMeal in favorites,
                                onFavoriteClick = {
                                    favorites =
                                        if (meal.idMeal in favorites) {
                                            favorites - meal.idMeal
                                        } else {
                                            favorites + meal.idMeal
                                        }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Garde la même largeur sur la dernière ligne
                        if (rowMeals.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: Meal,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {

            // Photo du repas provenant de l'API
            AsyncImage(
                model = meal.strMealThumb,
                contentDescription = meal.strMeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = meal.strMeal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FoodText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = meal.strCategory ?: "Meal",
                fontSize = 12.sp,
                color = FoodGray,
                maxLines = 1
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = FoodRed,
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(Modifier.width(3.dp))

                    Text(
                        text = meal.strArea ?: "Cuisine",
                        fontSize = 11.sp,
                        color = FoodText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = "Ajouter aux favoris",
                        tint = if (isFavorite) FoodRed
                        else FoodText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodgoBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    NavigationBar(
        containerColor = FoodRed,
        contentColor = Color.White
    ) {
        val items = listOf(
            Icons.Default.Home to "Home",
            Icons.Default.Favorite to "Favoris",
            Icons.Default.Receipt to "Commandes",
            Icons.Default.Person to "Profil"
        )

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        imageVector = item.first,
                        contentDescription = item.second
                    )
                },
                label = null,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FoodRed,
                    indicatorColor = Color.White,
                    unselectedIconColor = Color.White
                )
            )
        }
    }
}