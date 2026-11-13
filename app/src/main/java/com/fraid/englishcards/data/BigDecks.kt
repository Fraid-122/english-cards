package com.fraid.englishcards.data

/**
 * Large themed sets of words, phrases and idioms.
 * The themes follow what popular English-teaching YouTube channels cover most:
 * small talk, phrasal verbs, idioms, informal speech, travel, business English,
 * job interviews, emotions, money and technology.
 *
 * Each line is "english|ukrainian|example".
 */
object BigDecks {

    private fun deck(
        id: String,
        title: String,
        level: String,
        emoji: String,
        difficulty: Difficulty,
        lines: String,
    ): Deck = Deck(id, title, level, emoji, difficulty, lines.trimIndent().lines().filter { it.isNotBlank() })

    val all: List<Deck> = listOf(
        deck(
            "small-talk", "Щоденні фрази і small talk", "A2", "👋", Difficulty.EASY,
            """
            how's it going?|як справи?|Hi, Tom! How's it going?
            long time no see|давно не бачились|Long time no see! You look great.
            what have you been up to?|чим займався останнім часом?|So, what have you been up to?
            not much|нічого особливого|Not much, just working a lot.
            can't complain|не скаржуся|Can't complain, everything is fine.
            same here|у мене так само|I am tired. Same here.
            nice to meet you|приємно познайомитися|Nice to meet you, Anna.
            how was your weekend?|як минули вихідні?|How was your weekend?
            sounds good|звучить добре|Pizza tonight? Sounds good.
            no worries|нічого страшного|No worries, we have time.
            take care|бережи себе|See you later, take care!
            see you around|ще побачимось|I have to go. See you around!
            by the way|до речі|By the way, where do you live?
            to be honest|чесно кажучи|To be honest, I don't like it.
            I guess so|мабуть, так|Is it far? I guess so.
            it depends|залежить від обставин|Maybe, it depends on the weather.
            never mind|не зважай|Never mind, it is not important.
            my pleasure|мені було приємно|Thanks for the help. My pleasure.
            you're welcome|будь ласка (у відповідь на подяку)|Thank you! You're welcome.
            excuse me|перепрошую|Excuse me, where is the station?
            what do you do?|ким ти працюєш?|So, what do you do?
            I'm afraid not|боюся, що ні|Can you come? I'm afraid not.
            that's a pity|шкода|You can't come? That's a pity.
            congratulations|вітаю|Congratulations on your new job!
            good luck|хай щастить|Good luck with your exam!
            bless you|будь здоровий (коли хтось чхнув)|Bless you! Do you have a cold?
            have a nice day|гарного дня|Thank you, have a nice day!
            what's up?|що нового?|Hey, what's up?
            I see|зрозуміло|I see, so you work at night.
            exactly|саме так|Exactly, that is what I mean.
            of course|звичайно|Of course, I can help you.
            no way|не може бути|No way! You won the game?
            I have no idea|гадки не маю|I have no idea where he is.
            let me think|дай подумати|Let me think for a second.
            hold on|зачекай|Hold on, I will check.
            after you|після вас|After you, please.
            help yourself|пригощайся|Help yourself to some cake.
            make yourself at home|почувайся як удома|Come in and make yourself at home.
            what a surprise|який сюрприз|What a surprise to see you here!
            I'm sorry to hear that|мені шкода це чути|I'm sorry to hear that you are ill.
            it's up to you|вирішувати тобі|Tea or coffee? It's up to you.
            fair enough|справедливо, згоден|Fair enough, let's do it your way.
            I don't mind|я не проти|I don't mind if you open the window.
            just a moment|хвилинку|Just a moment, please.
            could you repeat that?|чи не могли б ви повторити?|Sorry, could you repeat that?
            what do you mean?|що ти маєш на увазі?|Sorry, what do you mean?
            keep in touch|залишайся на зв'язку|Let's keep in touch after the trip.
            say hi to|передавай привіт|Say hi to your sister for me.
            have a good one|гарного дня (розмовне)|Bye, have a good one!
            talk to you later|поговоримо пізніше|I am busy now, talk to you later.
            """,
        ),
        deck(
            "phrasal-everyday", "Фразові дієслова на щодень", "B1", "🧩", Difficulty.MEDIUM,
            """
            wake up|прокидатися|I wake up at seven every day.
            get up|вставати|I get up and make coffee.
            turn on|вмикати|Please turn on the light.
            turn off|вимикати|Turn off the TV before bed.
            put on|одягати|Put on your jacket, it is cold.
            take off|знімати (одяг); злітати|Take off your shoes at the door.
            look for|шукати|I look for my keys every morning.
            find out|дізнатися|I want to find out the truth.
            give up|здаватися, кидати|Never give up on your dream.
            go on|продовжувати|Please go on with your story.
            come back|повертатися|Come back before dinner.
            pick up|забрати, підняти|I will pick up the kids at five.
            drop off|підвезти, завезти|Can you drop off this letter?
            get along|ладнати|I get along with my neighbours.
            run out of|закінчитися (про запас)|We may run out of milk soon.
            look after|доглядати|She can look after the baby.
            look forward to|чекати з нетерпінням|I look forward to the weekend.
            put off|відкладати|Do not put off this call.
            call back|передзвонити|I will call back in ten minutes.
            hang up|покласти слухавку|Do not hang up, please.
            fill in|заповнити (форму)|Please fill in this form.
            try on|приміряти|Can I try on these shoes?
            throw away|викинути|Do not throw away that box.
            clean up|прибрати|Let's clean up the kitchen.
            set up|налаштувати, організувати|He will set up the new computer.
            figure out|з'ясувати, розібратися|I cannot figure out this puzzle.
            come up with|придумати|We need to come up with a plan.
            break down|зламатися|My car can break down any day.
            check in|зареєструватися (у готелі, на рейс)|We check in at two o'clock.
            check out|виселитися; глянути|We check out before noon.
            hang out|проводити час разом|We hang out at the park.
            work out|тренуватися; вдатися|I work out three times a week.
            show up|з'явитися, прийти|He did not show up yesterday.
            grow up|виростати|Children grow up so fast.
            bring up|виховувати; підняти (тему)|Do not bring up this topic.
            carry on|продовжувати|Carry on with your work.
            calm down|заспокоїтися|Calm down and breathe.
            cheer up|підбадьоритися|Cheer up, it will be fine.
            hurry up|поквапитися|Hurry up, we are late!
            slow down|сповільнитися|Slow down, you drive too fast.
            sit down|сісти|Sit down and relax.
            stand up|встати|Stand up when the teacher comes.
            get on|сісти (в автобус, потяг)|We get on the bus here.
            get off|вийти (з автобуса, потяга)|We get off at the next stop.
            go out|виходити кудись, гуляти|We go out every Friday.
            stay up|не лягати спати|I stay up late on weekends.
            take care of|піклуватися про|I take care of my parents.
            deal with|мати справу з, впоратися|I can deal with this problem.
            end up|опинитися врешті-решт|You may end up in the wrong city.
            make up|вигадати; помиритися|Do not make up excuses.
            """,
        ),
        deck(
            "idioms", "Найуживаніші ідіоми", "B1", "🎭", Difficulty.MEDIUM,
            """
            a piece of cake|дуже легко, раз плюнути|The test was a piece of cake.
            break the ice|розтопити лід, почати розмову|A joke can break the ice.
            once in a blue moon|дуже рідко|I eat fast food once in a blue moon.
            under the weather|нездужати|I feel under the weather today.
            hit the books|засісти за навчання|I have to hit the books tonight.
            cost an arm and a leg|коштувати шалені гроші|This phone can cost an arm and a leg.
            it's raining cats and dogs|ллє як з відра|Take an umbrella, it's raining cats and dogs.
            speak of the devil|про вовка промовка|Speak of the devil, here he comes!
            better late than never|краще пізно, ніж ніколи|You came! Better late than never.
            the ball is in your court|рішення тепер за тобою|I made my offer, the ball is in your court.
            kill two birds with one stone|вбити двох зайців одним пострілом|Let's kill two birds with one stone and shop on the way.
            let the cat out of the bag|вибовкати таємницю|Do not let the cat out of the bag about the party.
            bite the bullet|зціпити зуби і зробити|I will bite the bullet and call him.
            on the same page|мати однакове розуміння|Let's check that we are on the same page.
            get cold feet|злякатися в останню мить|Do not get cold feet before the wedding.
            hit the nail on the head|влучити в саму точку|You hit the nail on the head with that answer.
            call it a day|закінчити на сьогодні|We are tired, let's call it a day.
            break a leg|ні пуху ні пера|You have a show tonight? Break a leg!
            a blessing in disguise|нема лиха без добра|Losing that job was a blessing in disguise.
            miss the boat|проґавити нагоду|Buy tickets now or you will miss the boat.
            cut corners|робити абияк, заощаджуючи|Do not cut corners on safety.
            pulling my leg|розігрувати мене, жартувати|Stop it, you are pulling my leg!
            so far so good|поки що все добре|How is the new job? So far so good.
            the last straw|остання крапля|His rude answer was the last straw.
            a hot potato|гостра, незручна тема|This question is a hot potato in politics.
            spill the beans|видати секрет|Come on, spill the beans!
            sit on the fence|не ставати на жоден бік|You cannot sit on the fence forever.
            through thick and thin|і в горі, і в радості|She stayed with me through thick and thin.
            up in the air|ще не вирішено|Our holiday plans are still up in the air.
            out of the blue|несподівано, як грім серед ясного неба|He called me out of the blue.
            in hot water|у халепі|He is in hot water with his boss.
            a drop in the ocean|крапля в морі|This money is a drop in the ocean.
            actions speak louder than words|вчинки важать більше за слова|Help him today, actions speak louder than words.
            add fuel to the fire|підливати масла у вогонь|Do not add fuel to the fire with that comment.
            beat around the bush|ходити околясом|Do not beat around the bush, tell me the truth.
            get out of hand|вийти з-під контролю|The party can get out of hand quickly.
            give me a hand|допоможи мені|Can you give me a hand with this box?
            go the extra mile|докласти додаткових зусиль|Good teachers go the extra mile.
            keep an eye on|наглядати за|Please keep an eye on my bag.
            make a long story short|коротко кажучи|To make a long story short, we got lost.
            no pain, no gain|без праці нема результату|Keep training: no pain, no gain.
            on cloud nine|на сьомому небі від щастя|She was on cloud nine after the exam.
            play it by ear|діяти за обставинами|We have no plan, let's play it by ear.
            see eye to eye|сходитися в поглядах|We do not always see eye to eye.
            the tip of the iceberg|верхівка айсберга|These errors are only the tip of the iceberg.
            time flies|час летить|Time flies when you have fun.
            twist my arm|вмовити мене|Okay, you can twist my arm, I will stay.
            under pressure|під тиском|He works well under pressure.
            when pigs fly|коли рак на горі свисне|He will clean his room when pigs fly.
            your guess is as good as mine|я знаю не більше за тебе|Where is she? Your guess is as good as mine.
            """,
        ),
        deck(
            "informal", "Розмовна мова і сленг", "B1", "😎", Difficulty.MEDIUM,
            """
            gonna|збираюся (розмовне going to)|I am gonna call you later.
            wanna|хочу (розмовне want to)|Do you wanna go out?
            gotta|мушу (розмовне got to)|I gotta go now.
            kinda|ніби, трохи (розмовне kind of)|I am kinda tired today.
            cool|класно|That is a cool jacket.
            awesome|чудово, круто|The concert was awesome.
            no big deal|нічого страшного, дрібниця|It is no big deal, relax.
            hang on|зачекай|Hang on, I am coming.
            chill|розслабитися|Let's just chill at home.
            dude|чувак|Dude, where is my phone?
            guys|народ, хлопці|Hey guys, are you ready?
            I'm broke|я без грошей|I can't go, I'm broke.
            I'm starving|я вмираю з голоду|Let's eat, I'm starving.
            I'm beat|я виснажений|After work I'm beat.
            my bad|моя провина|Oh, my bad, I forgot.
            for real?|серйозно?|You moved to London? For real?
            no kidding|без жартів; та невже|It is cold today. No kidding!
            you bet|ще б пак|Are you coming? You bet!
            whatever|байдуже, як хочеш|Whatever, do what you want.
            what a mess|який безлад|Look at this room, what a mess!
            that sucks|це відстій, шкода|You lost your wallet? That sucks.
            I'm in|я в ділі, я з вами|A trip to the lake? I'm in.
            count me out|я пас|Karaoke again? Count me out.
            hit me up|напиши чи подзвони мені|Hit me up when you are free.
            catch up|надолужити, поспілкуватися|Let's catch up over coffee.
            grab a bite|перекусити|Do you want to grab a bite?
            binge-watch|дивитися серіал запоєм|I binge-watch shows on Sunday.
            couch potato|лежебока, домосід|Do not be a couch potato.
            a rip-off|обдиралівка|Ten dollars for water is a rip-off.
            freak out|панікувати|Do not freak out, it is only a spider.
            show off|вихвалятися|He likes to show off his car.
            screw up|напартачити|I do not want to screw up this time.
            nailed it|впорався ідеально|Great answer, you nailed it!
            piece of junk|мотлох|This old printer is a piece of junk.
            I'm down|я за, я не проти|Movie tonight? I'm down.
            it's on me|я пригощаю|Put your wallet away, it's on me.
            give it a shot|спробувати|I have never skied, but I will give it a shot.
            out of this world|неймовірний|This cake is out of this world.
            a no-brainer|очевидне рішення|Taking that job was a no-brainer.
            pretty much|майже, по суті|We are pretty much done.
            sort of|типу, начебто|I sort of like this song.
            you know|ну, знаєш|It was, you know, a strange day.
            I mean|я маю на увазі|I mean, it is not bad.
            big time|дуже, по-крупному|I owe you big time.
            hands down|беззаперечно|This is hands down the best pizza.
            low-key|без шуму, стримано|We had a low-key birthday party.
            legit|справжній, чесний|Is this website legit?
            bummer|облом, прикрість|No tickets left? What a bummer.
            hang in there|тримайся|Hang in there, it gets better.
            long story|довга історія|Why am I late? It is a long story.
            """,
        ),
        deck(
            "travel-phrases", "Подорожі: аеропорт, готель, ресторан", "A2", "✈️", Difficulty.EASY,
            """
            boarding pass|посадковий талон|Show your boarding pass, please.
            gate|вихід на посадку|Our gate is number twelve.
            carry-on|ручна поклажа|I only have a carry-on bag.
            check-in desk|стійка реєстрації|Where is the check-in desk?
            window seat|місце біля вікна|I would like a window seat.
            aisle seat|місце біля проходу|He prefers an aisle seat.
            customs|митниця|We went through customs quickly.
            one-way ticket|квиток в один бік|I need a one-way ticket to Lviv.
            return ticket|квиток туди й назад|A return ticket is cheaper.
            a single room|одномісний номер|I booked a single room.
            a double room|двомісний номер|We need a double room for two nights.
            reservation|бронювання|I have a reservation under my name.
            front desk|рецепція|Ask at the front desk.
            is breakfast included?|сніданок включено?|Excuse me, is breakfast included?
            a table for two|столик на двох|A table for two, please.
            the menu|меню|Can we see the menu, please?
            I would like|я хотів би|I would like a bowl of soup.
            the bill|рахунок|Can we have the bill, please?
            keep the change|решти не треба|Thank you, keep the change.
            tip|чайові|We left a small tip.
            tap water|вода з-під крана|Tap water is fine for me.
            takeaway|їжа з собою|Let's get a takeaway tonight.
            how much is it?|скільки це коштує?|I like this bag. How much is it?
            can I pay by card?|можна оплатити карткою?|Sorry, can I pay by card?
            cash|готівка|I have no cash with me.
            exchange rate|курс обміну|What is the exchange rate today?
            how do I get to|як мені дістатися до|How do I get to the city centre?
            go straight|ідіть прямо|Go straight for two blocks.
            turn left|поверніть ліворуч|Turn left at the bank.
            turn right|поверніть праворуч|Turn right after the bridge.
            it's on the corner|це на розі|The cafe? It's on the corner.
            a bus stop|автобусна зупинка|Is there a bus stop near here?
            platform|платформа|The train leaves from platform three.
            timetable|розклад руху|Check the timetable online.
            I'm lost|я заблукав|Excuse me, I'm lost.
            sightseeing|огляд визначних місць|We spent the day sightseeing.
            a guided tour|екскурсія з гідом|We booked a guided tour.
            souvenir|сувенір|I bought a souvenir for my mum.
            day trip|одноденна поїздка|We took a day trip to the sea.
            travel insurance|туристична страховка|Do you have travel insurance?
            rent a car|орендувати авто|We want to rent a car for a week.
            flight delay|затримка рейсу|There is a flight delay of two hours.
            lost luggage|загублений багаж|I need to report lost luggage.
            a connecting flight|рейс із пересадкою|I have a connecting flight in Vienna.
            jet lag|втома від зміни часових поясів|I still have jet lag.
            pack a suitcase|пакувати валізу|I pack a suitcase the night before.
            off the beaten track|осторонь туристичних маршрутів|We like places off the beaten track.
            tourist trap|пастка для туристів|That restaurant is a tourist trap.
            travel light|подорожувати без зайвих речей|I always travel light.
            safe trip|щасливої дороги|Have a safe trip!
            """,
        ),
        deck(
            "business", "Ділова англійська: зустрічі й листи", "B1", "💼", Difficulty.MEDIUM,
            """
            schedule a meeting|призначити зустріч|Let's schedule a meeting for Monday.
            agenda|порядок денний|What is on the agenda today?
            get started|починати|Okay, let's get started.
            keep it short|бути стислим|I will keep it short.
            key point|ключовий момент|The key point is the price.
            follow up|повернутися до питання, нагадати|I will follow up by email.
            action item|завдання до виконання|Each action item has an owner.
            deadline|крайній термін|The deadline is on Friday.
            on track|за графіком|The project is on track.
            behind schedule|із запізненням від графіка|We are two days behind schedule.
            ahead of schedule|з випередженням графіка|We finished ahead of schedule.
            touch base|вийти на зв'язок, звіритися|Let's touch base next week.
            in the loop|в курсі справ|Please keep me in the loop.
            up to speed|в курсі, ознайомлений|I will bring you up to speed after lunch.
            win-win|вигідно для всіх|This deal is a win-win.
            long-term|довгостроковий|We need a long-term plan.
            short-term|короткостроковий|It is only a short-term fix.
            budget|бюджет|The budget is very tight.
            cut costs|скоротити витрати|We have to cut costs this year.
            stakeholder|зацікавлена сторона|Every stakeholder must agree.
            feedback|відгук, зворотний зв'язок|Thank you for your feedback.
            I'm writing to|пишу вам, щоб|I'm writing to ask about the price.
            please find attached|у вкладенні надсилаю|Please find attached the report.
            kind regards|з повагою|Kind regards, Anna
            as soon as possible|якомога швидше|Please reply as soon as possible.
            let me know|дайте мені знати|Let me know if you need help.
            I'll get back to you|я повернуся до вас із відповіддю|I'll get back to you tomorrow.
            thanks in advance|заздалегідь дякую|Thanks in advance for your help.
            just a reminder|просто нагадую|Just a reminder about the meeting.
            sorry for the delay|вибачте за затримку|Sorry for the delay in my reply.
            to whom it may concern|усім, кого це стосується|To whom it may concern, I am writing about my order.
            could you clarify|чи не могли б ви уточнити|Could you clarify the last point?
            I agree with you|я згоден з вами|I agree with you on this.
            I see your point|я розумію вашу думку|I see your point, but it is risky.
            from my point of view|на мій погляд|From my point of view, it is too early.
            let's move on|перейдімо далі|Let's move on to the next topic.
            wrap up|завершувати, підсумовувати|Let's wrap up the meeting.
            take notes|робити нотатки|Who can take notes today?
            share the screen|поділитися екраном|Can you share the screen?
            you're on mute|у вас вимкнений мікрофон|Sorry, you're on mute.
            workload|робоче навантаження|My workload is heavy this month.
            priority|пріоритет|This task is a top priority.
            in charge of|відповідальний за|She is in charge of sales.
            report to|підпорядковуватися|I report to the head of design.
            team player|командний гравець|He is a real team player.
            think outside the box|мислити нестандартно|We need to think outside the box.
            the bottom line|суть, підсумок|The bottom line is that we need more time.
            back to square one|повернутися на початок|The test failed, so we are back to square one.
            a game changer|те, що докорінно змінює ситуацію|This tool is a game changer.
            learning curve|період освоєння нового|The new system has a steep learning curve.
            """,
        ),
        deck(
            "job-interview", "Співбесіда і робота", "B1", "🎯", Difficulty.MEDIUM,
            """
            resume|резюме|Send your resume by email.
            cover letter|супровідний лист|I wrote a short cover letter.
            job interview|співбесіда|I have a job interview tomorrow.
            apply for|подавати заявку на|I want to apply for this position.
            position|посада|The position is still open.
            tell me about yourself|розкажіть про себе|So, tell me about yourself.
            strengths|сильні сторони|What are your strengths?
            weaknesses|слабкі сторони|Be honest about your weaknesses.
            work experience|досвід роботи|I have five years of work experience.
            background|освіта й попередній досвід|My background is in design.
            skills|навички|These skills are useful for the job.
            fast learner|людина, яка швидко вчиться|I am a fast learner.
            detail-oriented|уважний до деталей|She is very detail-oriented.
            hard-working|працьовитий|He is honest and hard-working.
            reliable|надійний|We need a reliable person.
            work under pressure|працювати під тиском|I can work under pressure.
            meet deadlines|вкладатися в терміни|I always meet deadlines.
            teamwork|командна робота|Good teamwork saves time.
            problem-solving|розв'язання проблем|Problem-solving is my strong side.
            achievement|досягнення|My biggest achievement was a new website.
            responsibility|обов'язок, відповідальність|My main responsibility was support.
            full-time|повна зайнятість|It is a full-time job.
            part-time|часткова зайнятість|I work part-time at a cafe.
            remote work|віддалена робота|Remote work saves me two hours a day.
            salary expectations|зарплатні очікування|What are your salary expectations?
            benefits|соціальний пакет, пільги|The benefits include health insurance.
            notice period|термін попередження про звільнення|My notice period is two weeks.
            probation period|випробувальний термін|The probation period is three months.
            career goals|кар'єрні цілі|What are your career goals?
            in five years|через п'ять років|Where do you see yourself in five years?
            a good fit|той, хто добре підходить|I think I am a good fit for this team.
            why should we hire you?|чому ми маємо найняти вас?|Tell us, why should we hire you?
            hire|наймати|They want to hire two developers.
            quit|звільнитися|He decided to quit his job.
            get fired|бути звільненим|Nobody wants to get fired.
            promotion|підвищення на посаді|She got a promotion last month.
            raise|підвищення зарплати|I asked for a raise.
            references|рекомендації|Can you give us two references?
            qualified|кваліфікований|He is qualified for the role.
            responsible for|відповідальний за|I was responsible for the budget.
            day off|вихідний|I need a day off on Friday.
            sick leave|лікарняний|She is on sick leave this week.
            overtime|понаднормова робота|I do not mind some overtime.
            internship|стажування|I did an internship in Kyiv.
            job offer|пропозиція роботи|I got a job offer today!
            negotiate|домовлятися, вести переговори|You can negotiate the salary.
            start date|дата початку роботи|What is your start date?
            any questions for us?|маєте до нас запитання?|Do you have any questions for us?
            I look forward to hearing from you|чекаю на вашу відповідь|Thank you, I look forward to hearing from you.
            get the job|отримати роботу|I hope I get the job.
            """,
        ),
        deck(
            "emotions", "Емоції та характер", "B1", "💛", Difficulty.MEDIUM,
            """
            excited|у захваті, схвильований|I am excited about the trip.
            nervous|знервований|I feel nervous before exams.
            upset|засмучений|She was upset about the news.
            annoyed|роздратований|I get annoyed by loud music.
            exhausted|виснажений|I am exhausted after the run.
            relieved|той, хто відчув полегшення|I was relieved to see her safe.
            proud|гордий|I am proud of my son.
            embarrassed|збентежений, ніяково|I was embarrassed by my mistake.
            jealous|ревнивий, заздрісний|He is jealous of his brother.
            confused|спантеличений|I am confused by these rules.
            bored|знуджений|The kids are bored at home.
            delighted|дуже радий|We are delighted with the result.
            disappointed|розчарований|I am disappointed in the film.
            anxious|стривожений|She feels anxious about flying.
            grateful|вдячний|I am grateful for your help.
            lonely|самотній|He feels lonely in the new city.
            homesick|той, хто сумує за домом|I was homesick in my first year.
            overwhelmed|приголомшений, перевантажений|I feel overwhelmed with work.
            calm|спокійний|Stay calm and listen.
            confident|впевнений|She is confident in her skills.
            shy|сором'язливий|He was shy as a child.
            stubborn|впертий|My dad is very stubborn.
            generous|щедрий|She is generous with her time.
            selfish|егоїстичний|Do not be so selfish.
            honest|чесний|Be honest with me.
            polite|ввічливий|He is always polite.
            rude|грубий|That was a rude answer.
            easy-going|легкий у спілкуванні|My boss is easy-going.
            outgoing|товариський|She is friendly and outgoing.
            moody|з мінливим настроєм|He is moody in the morning.
            patient|терплячий|Be patient with beginners.
            curious|допитливий|Children are curious about everything.
            brave|хоробрий|It was a brave decision.
            lazy|ледачий|I feel lazy on Sundays.
            in a good mood|у гарному настрої|She is in a good mood today.
            in a bad mood|у поганому настрої|Do not talk to him, he is in a bad mood.
            fed up with|ситий по горло|I am fed up with this noise.
            can't stand|терпіти не можу|I can't stand cold coffee.
            get on my nerves|діяти мені на нерви|These ads get on my nerves.
            over the moon|у нестямі від радості|She was over the moon with the gift.
            down in the dumps|у пригніченому настрої|He has been down in the dumps all week.
            lose my temper|втратити самовладання|I rarely lose my temper.
            burst into tears|розплакатися|She may burst into tears.
            make my day|зробити мій день|Your message will make my day.
            a heart of gold|золоте серце|My grandma has a heart of gold.
            a pain in the neck|докучлива людина чи справа|This form is a pain in the neck.
            down to earth|простий, без пихи|He is famous but down to earth.
            a people person|товариська людина|A nurse must be a people person.
            cry over spilled milk|шкодувати за тим, чого не повернеш|Do not cry over spilled milk.
            keep your chin up|не вішай носа|Keep your chin up, you did well.
            """,
        ),
        deck(
            "money", "Гроші та покупки", "A2", "💳", Difficulty.EASY,
            """
            price|ціна|The price is too high.
            cheap|дешевий|This shirt is cheap.
            expensive|дорогий|The hotel is expensive.
            discount|знижка|Is there a discount for students?
            on sale|на розпродажі|These shoes are on sale.
            receipt|чек|Keep the receipt, please.
            refund|повернення коштів|I would like a refund.
            change|решта|Here is your change.
            wallet|гаманець|I left my wallet at home.
            credit card|кредитна картка|Do you take a credit card?
            afford|дозволити собі|I can't afford a new car.
            save money|заощаджувати гроші|I save money for a trip.
            spend money|витрачати гроші|I spend money on books.
            borrow|позичати (у когось)|Can I borrow ten dollars?
            lend|позичати (комусь)|I can lend you my bike.
            owe|бути винним|I owe you a coffee.
            pay back|повернути борг|I will pay back the money on Monday.
            bill|рахунок|The electricity bill is high.
            rent|орендна плата|The rent is due on the first.
            salary|зарплата|My salary comes every month.
            tax|податок|The price includes tax.
            a bargain|вигідна покупка|This coat was a bargain.
            worth it|варте того|The ticket is expensive but worth it.
            out of stock|немає в наявності|Sorry, this size is out of stock.
            in stock|є в наявності|Is this model in stock?
            fitting room|примірочна|Where is the fitting room?
            what size?|який розмір?|What size? Medium, please.
            it doesn't fit|не підходить за розміром|This jacket is small, it doesn't fit.
            can I return it?|чи можу я це повернути?|If it is too big, can I return it?
            I'm just looking|я просто дивлюся|No, thanks, I'm just looking.
            checkout|каса|Pay at the checkout.
            shopping cart|візок для покупок|Put it in the shopping cart.
            delivery|доставка|Delivery takes two days.
            free shipping|безкоштовна доставка|They offer free shipping.
            order online|замовляти онлайн|I order online every week.
            track my order|відстежити моє замовлення|How can I track my order?
            tight budget|обмежений бюджет|I am on a tight budget this month.
            make ends meet|зводити кінці з кінцями|It is hard to make ends meet.
            pay in cash|платити готівкою|I prefer to pay in cash.
            waste of money|марна трата грошей|That gadget is a waste of money.
            save for a rainy day|відкладати на чорний день|I save for a rainy day.
            break the bank|коштувати надто дорого|A weekend trip will not break the bank.
            for free|безкоштовно|You can park here for free.
            half price|за пів ціни|Everything is half price today.
            window shopping|розглядання вітрин|We went window shopping.
            impulse buy|спонтанна покупка|This lamp was an impulse buy.
            value for money|добре співвідношення ціни та якості|This phone is good value for money.
            shop around|порівнювати ціни в різних місцях|Shop around before you buy.
            split the bill|розділити рахунок|Let's split the bill.
            pocket money|кишенькові гроші|The kids get pocket money every week.
            """,
        ),
        deck(
            "tech", "Технології та інтернет", "B1", "💻", Difficulty.MEDIUM,
            """
            device|пристрій|This device is very light.
            screen|екран|The screen is too bright.
            charger|зарядний пристрій|I forgot my charger.
            battery life|час роботи батареї|The battery life is great.
            power bank|повербанк|Take a power bank with you.
            plug in|підключити до розетки|Plug in the laptop, please.
            log in|увійти в акаунт|Log in with your email.
            log out|вийти з акаунта|Do not forget to log out.
            sign up|зареєструватися|Sign up to get the newsletter.
            password|пароль|Choose a strong password.
            username|ім'я користувача|My username is short.
            download|завантажити (собі)|You can download the app for free.
            upload|завантажити (на сервер)|Upload your photo here.
            update|оновлення; оновити|Install the update tonight.
            install|встановити|It is easy to install this app.
            settings|налаштування|Open the settings and turn on sound.
            notification|сповіщення|I got a notification from the bank.
            back up|зробити резервну копію|Back up your files every week.
            restart|перезапустити|Try to restart the phone.
            crash|аварійно закритися|The app may crash on old phones.
            bug|помилка в програмі|We found a bug in the code.
            fix|виправити|They will fix it soon.
            feature|функція, можливість|I love this new feature.
            user-friendly|зручний для користувача|The menu is user-friendly.
            wireless|бездротовий|I use wireless headphones.
            Wi-Fi connection|з'єднання Wi-Fi|The Wi-Fi connection is slow.
            out of range|поза зоною досяжності|My phone is out of range here.
            low battery|низький заряд|I have a low battery, call me later.
            screenshot|знімок екрана|Send me a screenshot.
            link|посилання|Click the link below.
            search engine|пошукова система|Use a search engine to find it.
            look it up|пошукай це|I do not know, look it up online.
            go viral|стати вірусним|This video can go viral.
            subscribe|підписатися|Subscribe to my channel.
            social media|соціальні мережі|I spend less time on social media now.
            scroll|гортати стрічку|I scroll through news in bed.
            swipe|провести пальцем по екрану|Swipe left to delete.
            tap|торкнутися, натиснути|Tap the button twice.
            attach a file|прикріпити файл|Do not forget to attach a file.
            spam|спам|This email looks like spam.
            scam|шахрайство|Be careful, it is a scam.
            privacy|приватність|Privacy is important online.
            data|дані|The app uses a lot of data.
            cloud storage|хмарне сховище|I keep photos in cloud storage.
            video call|відеодзвінок|Let's have a video call tonight.
            freeze|зависнути|Old laptops often freeze.
            turn it off and on again|вимкни й увімкни знову|Just turn it off and on again.
            out of date|застарілий|This map is out of date.
            state-of-the-art|найсучасніший|They have a state-of-the-art lab.
            cutting-edge|передовий|It is a cutting-edge technology.
            """,
        ),
    )
}
