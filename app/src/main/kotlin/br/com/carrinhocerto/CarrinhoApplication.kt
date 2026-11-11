package br.com.carrinhocerto

import android.app.Application
import androidx.room.Room
import br.com.carrinhocerto.data.*
import br.com.carrinhocerto.ui.ShopViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class CarrinhoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CarrinhoApplication)
            modules(module {
                single { Room.databaseBuilder(get(), ShopDatabase::class.java, "carrinho.db").build() }
                single { Settings(get()) }
                single { retrofit("https://world.openfoodfacts.org/").create(FoodApi::class.java) }
                single { ShopRepository(get(), get(), get(), get()) }
                factory { ShopViewModel(get()) }
            })
        }
    }
}
