# Foodgo

Foodgo est une petite application Android de commande de repas.
On parcourt un menu de près de 800 plats, on regarde le détail d'un plat, on l'ajoute au panier et on passe commande.

Projet réalisé dans le cadre du cours de développement mobile.

---

## Ce que fait l'application

- **Écran de démarrage** animé avec le logo Foodgo.
- **Accueil** : barre de recherche, catégories (bœuf, poulet, desserts…) et liste des plats.
- **Tri** : les mieux notés, prix croissant, prix décroissant ou par nom.
- **Fiche d'un plat** : grande photo, prix, note, temps de préparation, calories, ingrédients et recette.
- **Favoris** : on garde ses plats préférés avec le cœur.
- **Panier** : on ajoute des plats avec le bouton **+**, on change les quantités.
  La livraison est offerte dès 30 €, sinon elle coûte 2,99 €.
- **Commandes** : historique des commandes passées, avec un bouton pour recommander.
- **Profil** : nom, e-mail, téléphone et adresse de livraison.

---

## D'où viennent les données ?

### Pourquoi Supabase ?

Au départ, l'application utilisait l'API gratuite **TheMealDB**.
Le problème : dans l'application, elle ne renvoyait que **4 plats**, ce qui était trop peu pour avoir un vrai menu.

Nous avons donc créé notre propre base de données en ligne sur **Supabase**, avec une table `meals` qui contient près de **800 plats** (nom, catégorie, photo, recette, ingrédients, prix, note, temps de préparation, calories).

| Données | Où elles sont stockées |
|---|---|
| Les plats (menu) | En ligne, sur **Supabase** (table `meals`) |
| Favoris, panier, commandes, profil | Sur le téléphone, avec **Room** |

Les plats sont téléchargés depuis Supabase à chaque ouverture de l'application.
Il n'y a rien à configurer : il suffit d'avoir **Internet** au lancement.
Ce qui est propre à l'utilisateur (son panier, ses favoris…) reste sur son téléphone.

### L'API Supabase

- **URL du projet** : `https://kexnkzgnomnbdkymiyki.supabase.co`
- **Point d'accès des plats** : `https://kexnkzgnomnbdkymiyki.supabase.co/rest/v1/meals`
- **Clé publique** : `sb_publishable_jTLUyRYG0a402DP7kJHOqw_z_1y-kTo`
- **Table** : `meals`

La clé est envoyée dans l'en-tête `apikey`. C'est une clé **publique**, prévue pour être utilisée dans une application : elle permet seulement de lire les plats.

**Tester l'API dans un navigateur** (le plus simple) : ouvrir ce lien, la clé est passée dans l'URL.

```
https://kexnkzgnomnbdkymiyki.supabase.co/rest/v1/meals?select=name,category,price,thumb&apikey=sb_publishable_jTLUyRYG0a402DP7kJHOqw_z_1y-kTo
```

**Tester l'API dans un terminal Windows (PowerShell)** : utiliser `curl.exe` (dans PowerShell, `curl` tout court est un autre outil qui n'accepte pas `-H`).

```
curl.exe "https://kexnkzgnomnbdkymiyki.supabase.co/rest/v1/meals?select=name,category,price,thumb" -H "apikey: sb_publishable_jTLUyRYG0a402DP7kJHOqw_z_1y-kTo"
```

Sur Mac ou Linux, la même commande fonctionne avec `curl` au lieu de `curl.exe`.

Dans le code, l'appel se trouve dans `MealApi.kt`.

### Éviter la mise en pause de Supabase

Sur l'offre gratuite, Supabase met le projet en pause après 7 jours sans activité. L'application ne pourrait alors plus charger les plats.
Pour l'éviter, une tâche automatique GitHub Actions (`.github/workflows/supabase-keepalive.yml`) interroge l'API une fois par jour.
On peut la lancer à la main depuis l'onglet **Actions** du dépôt GitHub (*Supabase keep-alive → Run workflow*).

---

## Lancer le projet

1. Cloner le dépôt :
   ```
   git clone https://github.com/chaimaaskri7/projet-mobile.git
   ```
2. Ouvrir le dossier dans **Android Studio**.
3. Attendre la fin de la synchronisation Gradle.
4. Choisir un émulateur (ou brancher un téléphone) et cliquer sur **Run**.

> Si Android Studio affiche **« Incompatible Gradle JVM version »** :
> aller dans *Settings → Build Tools → Gradle → Gradle JDK* et choisir **Java 17** (par exemple `jbr-17`).

**Configuration minimale** : Android 7.0 (API 24) ou plus récent.

---

## Technologies utilisées

- **Kotlin** et **Jetpack Compose** pour l'interface
- **Supabase** pour la base de données en ligne des plats
- **Retrofit** pour appeler l'API Supabase
- **Room** pour les données enregistrées sur le téléphone
- **Coil** pour charger les images
- **ViewModel** et **Flow** pour mettre à jour l'écran automatiquement

---

## Organisation du code

```
app/src/main/java/com/example/projet_mobile/
├── MainActivity.kt         → démarrage, splash screen et navigation
├── FoodgoHomeScreen.kt     → écran d'accueil
├── MealDetailScreen.kt     → fiche détaillée d'un plat
├── TabScreens.kt           → favoris, panier, commandes, profil
├── Components.kt           → éléments réutilisables (cartes, boutons…)
├── FoodViewModel.kt        → logique des écrans
├── MealApi.kt              → appel à l'API Supabase
├── Meal.kt                 → format des plats reçus de Supabase
└── data/
    ├── Entities.kt         → modèles de données
    ├── FoodDatabase.kt     → base Room (favoris, panier, commandes, profil)
    └── FoodRepository.kt   → fait le lien entre Supabase et Room
```

---

## Crédits

Les photos et la base des recettes proviennent de [TheMealDB](https://www.themealdb.com).

---

Réalisé par **Chaima Askri** — ISIS Castres, Santé Numérique, Promotion 2028
