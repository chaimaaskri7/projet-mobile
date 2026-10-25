package com.example.projet_mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projet_mobile.data.CartLine
import com.example.projet_mobile.data.CategoryEntity
import com.example.projet_mobile.data.FoodDatabase
import com.example.projet_mobile.data.FoodRepository
import com.example.projet_mobile.data.MealEntity
import com.example.projet_mobile.data.OrderEntity
import com.example.projet_mobile.data.ProfileEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode(val label: String) {
    POPULAR("Les mieux notés"),
    PRICE_ASC("Prix croissant"),
    PRICE_DESC("Prix décroissant"),
    NAME("Nom (A → Z)")
}

data class SyncState(
    val loading: Boolean = false,
    val offline: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FoodViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FoodRepository(FoodDatabase.get(application))

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    // ---------- Filtres de l'accueil ----------

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.POPULAR)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _sync = MutableStateFlow(SyncState())
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // ---------- Données (toutes lues depuis Room) ----------

    val categories: StateFlow<List<CategoryEntity>> = repo.categories.state(emptyList())

    /** null = pas encore chargé (pour afficher le squelette au lieu de "aucun résultat"). */
    val meals: StateFlow<List<MealEntity>?> =
        combine(
            _selectedCategory,
            _query.debounce(250).map { it.trim() }.onStart { emit("") }.distinctUntilChanged()
        ) { category, query -> category to query }
            .flatMapLatest { (category, query) -> repo.meals(category, query) }
            .combine(_sortMode) { list, sort ->
                when (sort) {
                    SortMode.POPULAR -> list
                    SortMode.PRICE_ASC -> list.sortedBy { it.price }
                    SortMode.PRICE_DESC -> list.sortedByDescending { it.price }
                    SortMode.NAME -> list.sortedBy { it.name.lowercase() }
                }
            }
            .state(null)

    val favoriteIds: StateFlow<Set<String>> =
        repo.favoriteIds.map { it.toSet() }.state(emptySet())

    val favorites: StateFlow<List<MealEntity>> = repo.favoriteMeals.state(emptyList())

    val cart: StateFlow<List<CartLine>> = repo.cart.state(emptyList())

    val cartCount: StateFlow<Int> = repo.cart.map { lines -> lines.sumOf { it.quantity } }.state(0)

    val orders: StateFlow<List<OrderEntity>> = repo.orders.state(emptyList())

    val profile: StateFlow<ProfileEntity?> = repo.profile.state(null)

    val mealCount: StateFlow<Int> = repo.mealCount.state(0)

    fun meal(id: String): Flow<MealEntity?> = repo.meal(id)

    init {
        sync(force = false)
    }

    // ---------- Actions ----------

    fun selectCategory(name: String) {
        _selectedCategory.value = name
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun setSort(mode: SortMode) {
        _sortMode.value = mode
    }

    /** force = false : on n'appelle l'API que si Room est vide. */
    fun sync(force: Boolean) {
        if (_sync.value.loading) return
        viewModelScope.launch {
            if (!force && repo.hasCache()) {
                _sync.value = SyncState()
                return@launch
            }
            _sync.value = _sync.value.copy(loading = true, error = null)
            val result = runCatching { repo.refreshCatalog() }
            result
                .onSuccess { count ->
                    _sync.value = SyncState()
                    if (force) _messages.tryEmit("Menu mis à jour : $count plats")
                }
                .onFailure {
                    val hasCache = repo.hasCache()
                    _sync.value = SyncState(
                        offline = hasCache,
                        error = if (hasCache) null
                        else "Impossible de charger le menu.\nVérifie ta connexion Internet."
                    )
                    if (hasCache) _messages.tryEmit("Hors ligne : affichage du menu enregistré")
                }
        }
    }

    fun toggleFavorite(meal: MealEntity) {
        viewModelScope.launch {
            val added = repo.toggleFavorite(meal.id)
            _messages.tryEmit(
                if (added) "Ajouté aux favoris ❤" else "Retiré des favoris"
            )
        }
    }

    fun addToCart(meal: MealEntity, quantity: Int = 1, notify: Boolean = true) {
        viewModelScope.launch {
            repo.addToCart(meal.id, quantity)
            if (notify) _messages.tryEmit("${meal.name} ajouté au panier")
        }
    }

    fun setQuantity(mealId: String, quantity: Int) {
        viewModelScope.launch { repo.setQuantity(mealId, quantity) }
    }

    fun removeFromCart(mealId: String) {
        viewModelScope.launch { repo.removeFromCart(mealId) }
    }

    fun placeOrder(onPlaced: (Long) -> Unit) {
        viewModelScope.launch {
            val address = profile.value?.address.orEmpty()
            val id = repo.placeOrder(address)
            if (id != null) onPlaced(id)
        }
    }

    fun reorder(order: OrderEntity) {
        viewModelScope.launch {
            repo.reorder(order)
            _messages.tryEmit("Commande #${order.id} ajoutée au panier")
        }
    }

    fun clearOrders() {
        viewModelScope.launch {
            repo.clearOrders()
            _messages.tryEmit("Historique supprimé")
        }
    }

    fun saveProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repo.saveProfile(profile)
            _messages.tryEmit("Profil enregistré")
        }
    }
}
