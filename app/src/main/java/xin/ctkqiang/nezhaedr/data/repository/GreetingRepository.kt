package xin.ctkqiang.nezhaedr.data.repository

import xin.ctkqiang.nezhaedr.data.model.GreetingModel

interface GreetingRepository {
    fun getGreeting(): GreetingModel
}

class GreetingRepositoryImpl : GreetingRepository {
    override fun getGreeting(): GreetingModel {
        return GreetingModel(name = "Android")
    }
}
