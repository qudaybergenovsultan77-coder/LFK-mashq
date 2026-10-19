package uz.kozboyin.mashq

enum class Anim { BLINK, UP_DOWN, LEFT_RIGHT, CIRCLE, TILT, NOD, TURN }

data class Exercise(
    val name: String,
    val desc: String,
    val anim: Anim,
    val seconds: Int = 20
)

val EXERCISES = listOf(
    Exercise("Ko'zni pirpiratish", "Ko'zlaringizni tez-tez va yengil pirpiratib turing. Bu ko'z namligini tiklaydi.", Anim.BLINK),
    Exercise("Yuqoriga va pastga qarash", "Boshingizni qimirlatmasdan, ko'zlaringiz bilan sekin yuqoriga, so'ng pastga qarang.", Anim.UP_DOWN),
    Exercise("Chapga va o'ngga qarash", "Boshingiz joyida tursin. Ko'zlaringiz bilan sekin chapga, so'ng o'ngga qarang.", Anim.LEFT_RIGHT),
    Exercise("Ko'z bilan aylana chizish", "Ko'zlaringiz bilan katta aylana chizing. Yarmidan keyin teskari tomonga aylantiring.", Anim.CIRCLE),
    Exercise("Bo'yinni yonga egish", "Quloqni yelkaga yaqinlashtiring: avval chapga, keyin o'ngga. Yelkangizni ko'tarmang.", Anim.TILT),
    Exercise("Bo'yinni oldinga va orqaga", "Iyagingizni sekin ko'kragingizga yaqinlashtiring, so'ng boshingizni yengil orqaga qiling.", Anim.NOD),
    Exercise("Boshni yon tomonga burish", "Boshingizni sekin chapga, so'ng o'ngga buring. Harakat silliq bo'lsin, og'riq bo'lmasin.", Anim.TURN)
)
