package com.example.webflux.repository

import com.example.webflux.controller.model.Plant
import kotlinx.coroutines.delay
import org.springframework.stereotype.Repository

@Repository
class PlantRepository {
    private val plants: MutableMap<Long, Plant> = mutableMapOf(
        1L to Plant(
            id = "1",
            name = "Монстера деликатесная",
            description = "Популярная тропическая лиана с крупными резными листьями. Быстро растет и создает эффектный акцент в интерьере. Идеальна для больших помещений.",
            price = 1500.0,
            images = listOf(
                "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1614594895304-fe7116ac3b58?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1545241047-6083a3684587?w=400&h=300&fit=crop"
            ),
            category = "Лианы",
            difficulty = "Легко"
        ),
        2L to Plant(
            id = "2",
            name = "Фикус лировидный",
            description = "Элегантное дерево с большими листьями в форме скрипки. Становится настоящей звездой любого интерьера. Требует яркого света и регулярного полива.",
            price = 2200.0,
            images = listOf(
                "https://images.unsplash.com/photo-1593482892290-f54927ae1bb6?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1592150621744-aca64f48394a?w=400&h=300&fit=crop"
            ),
            category = "Декоративнолиственные",
            difficulty = "Средне"
        ),
        3L to Plant(
            id = "3",
            name = "Сансевиерия (Щучий хвост)",
            description = "Неприхотливое растение с вертикальными жесткими листьями. Прекрасно очищает воздух и выдерживает недостаток полива. Идеально для начинающих.",
            price = 800.0,
            images = listOf(
                "https://images.unsplash.com/photo-1593482892290-f54927ae1bb6?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1509937528035-ad76254b0356?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1632207691143-643e2985c0f9?w=400&h=300&fit=crop"
            ),
            category = "Суккуленты",
            difficulty = "Легко"
        ),
        4L to Plant(
            id = "4",
            name = "Нефролепис (Бостонский папоротник)",
            description = "Пышный папоротник с изящными перистыми листьями. Создает атмосферу тропического леса и увлажняет воздух. Любит частое опрыскивание.",
            price = 1200.0,
            images = listOf(
                "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1597165194215-65eedfbf8de7?w=400&h=300&fit=crop"
            ),
            category = "Папоротники",
            difficulty = "Средне"
        ),
        5L to Plant(
            id = "5",
            name = "Спатифиллум (Женское счастье)",
            description = "Грациозное растение с белыми цветами-парусами. Отлично растет в тени и сигнализирует о необходимости полива. Цветет при хорошем уходе.",
            price = 900.0,
            images = listOf(
                "https://images.unsplash.com/photo-1593691509543-c55fb32d8de5?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1597165194215-65eedfbf8de7?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1587334207984-1e5b6e4e2e28?w=400&h=300&fit=crop"
            ),
            category = "Цветущие",
            difficulty = "Легко"
        ),
        6L to Plant(
            id = "6",
            name = "Калатея",
            description = "Экзотическая красавица с узорчатыми листьями. Листья двигаются в течение дня, поднимаясь вечером. Требует повышенной влажности воздуха.",
            price = 1800.0,
            images = listOf(
                "https://images.unsplash.com/photo-1586685281522-c072aeebee21?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1591958911259-bee2173bdccc?w=400&h=300&fit=crop"
            ),
            category = "Декоративнолиственные",
            difficulty = "Сложно"
        ),
        7L to Plant(
            id = "7",
            name = "Алоэ вера",
            description = "Лечебный суккулент с мясистыми листьями. Неприхотлив и полезен для ухода за кожей. Отлично переносит засуху и яркое солнце.",
            price = 600.0,
            images = listOf(
                "https://images.unsplash.com/photo-1596548438137-d51ea5c83ca5?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1509423350716-97f9360b4e09?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1598880940371-c756e015faf1?w=400&h=300&fit=crop"
            ),
            category = "Суккуленты",
            difficulty = "Легко"
        ),
        8L to Plant(
            id = "8",
            name = "Драцена",
            description = "Элегантное растение-дерево с пучком узких листьев на верхушке. Неприхотливо и растет в полутени. Прекрасно подходит для офисов и гостиных.",
            price = 1100.0,
            images = listOf(
                "https://images.unsplash.com/photo-1597165194215-65eedfbf8de7?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1610768764270-790fbec18178?w=400&h=300&fit=crop"
            ),
            category = "Декоративнолиственные",
            difficulty = "Легко"
        ),
        9L to Plant(
            id = "9",
            name = "Тропический лес в стекле",
            description = "Роскошная композиция из мха, папоротника и фиттонии в геометрическом флорариуме. Создает атмосферу влажных тропиков. Самоувлажняющаяся экосистема.",
            price = 3500.0,
            images = listOf(
                "https://images.unsplash.com/photo-1545241047-6083a3684587?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=400&h=300&fit=crop"
            ),
            category = "Флорариум",
            difficulty = "Не требует ухода"
        ),
        10L to Plant(
            id = "10",
            name = "Пустынный пейзаж",
            description = "Минималистичная композиция из суккулентов и кактусов на песчаной подложке. Идеальна для современных интерьеров. Поливать раз в месяц.",
            price = 2800.0,
            images = listOf(
                "https://images.unsplash.com/photo-1459156212016-c812468e2115?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1496192521521-98b789c0c24e?w=400&h=300&fit=crop"
            ),
            category = "Флорариум",
            difficulty = "Минимальный уход"
        ),
        11L to Plant(
            id = "11",
            name = "Мини-джунгли",
            description = "Плотная композиция из миниатюрных тропических растений с декоративными камнями и корягой. Вечнозеленые джунгли в бутылке. Полностью автономная экосистема.",
            price = 4200.0,
            images = listOf(
                "https://images.unsplash.com/photo-1466781783364-36c955e42a7f?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1452827073306-6e6e661baf57?w=400&h=300&fit=crop"
            ),
            category = "Флорариум",
            difficulty = "Не требует ухода"
        ),
        12L to Plant(
            id = "12",
            name = "Лесной мох",
            description = "Композиция из трех видов мха с натуральной корягой и камнями. Напоминает лесную поляну после дождя. Мох сохраняет свежесть благодаря герметичному стеклу.",
            price = 3200.0,
            images = listOf(
                "https://images.unsplash.com/photo-1470058869958-2a77ade41c02?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1509423350716-97f9360b4e09?w=400&h=300&fit=crop"
            ),
            category = "Флорариум",
            difficulty = "Не требует ухода"
        ),
        13L to Plant(
            id = "13",
            name = "Воздушный сад",
            description = "Подвесной флорариум с тилландсиями - воздушными растениями, которые не нуждаются в почве. Стильный элемент декора. Опрыскивание раз в неделю.",
            price = 2500.0,
            images = listOf(
                "https://images.unsplash.com/photo-1509587584298-0f3b3a3a1797?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1542223616-9de9adb5e3e8?w=400&h=300&fit=crop"
            ),
            category = "Флорариум",
            difficulty = "Минимальный уход"
        ),
        14L to Plant(
            id = "14",
            name = "Каменный садик",
            description = "Элегантная композиция из эхеверий, хавортий и декоративных минералов в широком стеклянном контейнере. Японская эстетика в сочетании с живыми растениями.",
            price = 3800.0,
            images = listOf(
                "https://images.unsplash.com/photo-1565008576549-57569a49371d?w=400&h=300&fit=crop",
                "https://images.unsplash.com/photo-1591825729269-caeb344f6df2?w=400&h=300&fit=crop"
            ),
            category = "Флорариум",
            difficulty = "Минимальный уход"
        ),
        15L to Plant(
            id = "15",
            name = "Флорариум своими руками",
            description = "Пошаговый мастер-класс по созданию флорариумов. Узнайте, как выбрать растения, подготовить грунт, создать композицию и ухаживать за мини-экосистемой в стекле.",
            price = 2500.0,
            images = listOf(
                "https://images.unsplash.com/photo-1459156212016-c812468e2115?w=400&h=300&fit=crop"
            ),
            category = "Мастер-класс",
            difficulty = "Для начинающих",
            type = "COURSE",
            duration = 90,
            videoUrl = "kinescope_stub_florarium_basics",
            previewUrl = null
        ),
        16L to Plant(
            id = "16",
            name = "Уход за тропическими растениями",
            description = "Секреты выращивания капризных тропических растений в домашних условиях. Полив, влажность, освещение, пересадка, борьба с вредителями.",
            price = 1800.0,
            images = listOf(
                "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?w=400&h=300&fit=crop"
            ),
            category = "Мастер-класс",
            difficulty = "Средний уровень",
            type = "COURSE",
            duration = 75,
            videoUrl = "kinescope_stub_tropical_care",
            previewUrl = null
        ),
        17L to Plant(
            id = "17",
            name = "Размножение комнатных растений",
            description = "Все способы размножения комнатных растений: черенками, листьями, делением, отводками. Укоренение в воде и грунте. Создание мини-парничка.",
            price = 1500.0,
            images = listOf(
                "https://images.unsplash.com/photo-1545241047-6083a3684587?w=400&h=300&fit=crop"
            ),
            category = "Мастер-класс",
            difficulty = "Для начинающих",
            type = "COURSE",
            duration = 60,
            videoUrl = "kinescope_stub_propagation",
            previewUrl = null
        )
    )

    suspend fun findAll(): List<Plant> {
        return plants.values.toList()
    }

    suspend fun findById(id: Long): Plant? {
        return plants[id]
    }
}
