import com.google.gson.Gson
import com.zealsinger.interview_butler.cache.Event

class EventDataManager {
    companion object {
        private val gson = Gson()

        fun parseFromJson(jsonString: String): List<Event> {
            val typeToken = object : com.google.gson.reflect.TypeToken<List<Event>>() {}.type
            return gson.fromJson(jsonString, typeToken)
        }

        fun eventListToJson(eventList: List<Event>): String {
            return gson.toJson(eventList)
        }
    }
}