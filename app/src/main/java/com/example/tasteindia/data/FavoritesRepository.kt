package com.example.tasteindia.data

import com.example.tasteindia.data.local.FavoriteDao
import com.example.tasteindia.data.local.FavoriteEntity
import com.example.tasteindia.domain.MealSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Favourites never touch the network, so they work offline and survive relaunch. */
interface FavoritesRepository {
    val favourites: Flow<List<MealSummary>>
    suspend fun toggle(meal: MealSummary)
    suspend fun remove(id: String)
}

class RoomFavoritesRepository(private val dao: FavoriteDao) : FavoritesRepository {

    override val favourites: Flow<List<MealSummary>> =
        dao.observeAll().map { rows -> rows.map { MealSummary(it.id, it.name, it.thumbnailUrl) } }

    override suspend fun toggle(meal: MealSummary) {
        if (dao.exists(meal.id)) {
            dao.delete(meal.id)
        } else {
            dao.upsert(FavoriteEntity(meal.id, meal.name, meal.thumbnailUrl))
        }
    }

    override suspend fun remove(id: String) = dao.delete(id)
}
