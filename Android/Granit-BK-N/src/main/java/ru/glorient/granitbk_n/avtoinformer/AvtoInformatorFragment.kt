package ru.glorient.granitbk_n.avtoinformer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.Subscribe
import org.json.JSONObject
import ru.glorient.bkn.ServiceManager
import ru.glorient.granitbk_n.MainActivity
import ru.glorient.granitbk_n.MessageEvent
import ru.glorient.granitbk_n.R
import ru.glorient.granitbk_n.accessory.Accessory
import ru.glorient.granitbk_n.accessory.UpdateListListener
import ru.glorient.granitbk_n.accessory.withArguments
import ru.glorient.granitbk_n.adapters.StopAdapter
import ru.glorient.granitbk_n.databinding.FragmentAvtoinformerBinding

class AvtoInformatorFragment : Fragment(R.layout.fragment_avtoinformer), UpdateListListener {
    private var stopAdapter: StopAdapter? = null
    private val stopListViewModel: AvtoInformatorViewModel by viewModels()

    private var _binding: FragmentAvtoinformerBinding? = null
    private val binding: FragmentAvtoinformerBinding get() = _binding!!

    private lateinit var busStopList: RecyclerView
    private lateinit var textViewStartStop: TextView
    private lateinit var textViewFinishStop: TextView

    // Список остановок маршрута
    private var listStop = mutableListOf<Stop>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Байдим вьюху (ViewBinding)
        _binding = FragmentAvtoinformerBinding.bind(view)
        // Находим список
        busStopList = view.findViewById(R.id.busStopList)
        // TextView начало маршрута
        textViewStartStop = view.findViewById(R.id.startStop)
        // TextView конец маршрута
        textViewFinishStop = view.findViewById(R.id.finalStop)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "onActivityCreated activity=${hashCode()}")

        // EventBus для прокидывания данных между фрагментами или активити
//        EventBus.getDefault().register(this)
//        if (BuildConfig.DEBUG) {

        // Инициализируем адаптер
        initList()

        // Ловим нажатие на изменение направления движения
        binding.routeBidir.setOnClickListener {
            Accessory().screenBlock(true, requireActivity().window)
            toggle()

            // Меняем маршрут и запрашиваем данные через пол секунды
            Handler(Looper.getMainLooper()).postAtTime({
                stopListViewModel.requestStops()
            }, SystemClock.uptimeMillis() + 500L)
            Accessory().screenBlock(false, requireActivity().window)
        }

        // Получаем список остановок
        stopListViewModel.requestStops()
        // Подписываемся через лайвдату на изменение списка
        stopListViewModel.stops
            .observe(viewLifecycleOwner) { newStop: List<Stop> ->
                listStop = mutableListOf()
                val listStopScreen = mutableListOf<Stop>()
                newStop.forEachIndexed { ind, it: Stop ->
                    // Сравниваем по индексу и заполняем начало и конец маршрута
                    when (ind) {
                        0 -> {
                            textViewStartStop.text =
                                (it as? Stop.DefaultStop)?.name ?: ((it as? Stop.NextStop)?.name)
                        }
                        newStop.size - 1 -> {
                            textViewFinishStop.text =
                                (it as? Stop.DefaultStop)?.name ?: ((it as? Stop.NextStop)?.name)
                        }
                        else -> {
                            // Заполняем список остановок для адаптера
                            listStopScreen.add(it)
                        }
                    }
                    // Заполняем список остановок
                    this.listStop.add(it)
                }

                // Выводим в адаптер
                stopAdapter?.items = listStopScreen
            }

        // Нажатие на стартовое поле маршрута (при ручном управлении)
        textViewStartStop.setOnClickListener {
            if (!MainActivity.flagSelectedButtonAvto) {
                val id =
                    (listStop[0] as? Stop.DefaultStop)?.id ?: ((listStop[0] as? Stop.NextStop)?.id)
                if (id != null) {
                    play(id)
                }
            }
        }

        // Нажатие на финишное поле маршрута (при ручном управлении)
        textViewFinishStop.setOnClickListener {
            if (!MainActivity.flagSelectedButtonAvto) {
                val id = (listStop[listStop.lastIndex] as? Stop.DefaultStop)?.id
                    ?: ((listStop[listStop.lastIndex] as? Stop.NextStop)?.id)
                if (id != null) {
                    play(id)
                }
            }
        }
    }

    // Инициализируем адаптер
    private fun initList() {
        stopAdapter = StopAdapter { position ->
            if (!MainActivity.flagSelectedButtonAvto) {
                Accessory().screenBlock(true, requireActivity().window)

                if (position == -1) {
                    Log.e(TAG, "Ошибка position = $position")
                    Accessory().screenBlock(false, requireActivity().window)
                    return@StopAdapter
                }
                Log.d(TAG, "initList position = $position ")
                Log.d(TAG, "initList listStop = ${listStop[position]} ")
                val id = (listStop[position + 1] as? Stop.DefaultStop)?.id
                    ?: ((listStop[position + 1] as? Stop.NextStop)?.id)
                if (id != null) {
                    play(id)
                }
                Accessory().screenBlock(false, requireActivity().window)
            }
        }

        with(busStopList) {
            adapter = stopAdapter
            // Кастомизируем LinearLayoutManager для медленного прокручивания списка
            val customLayoutManager: LinearLayoutManager? =
                object : LinearLayoutManager(requireContext()) {
                    override fun smoothScrollToPosition(
                        recyclerView: RecyclerView,
                        state: RecyclerView.State,
                        position: Int
                    ) {
                        val smoothScroller: LinearSmoothScroller =
                            object : LinearSmoothScroller(requireContext()) {
                                private val SPEED = 300f // Change this value (default=25f)
                                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                                    return SPEED / displayMetrics.densityDpi
                                }
                            }
                        smoothScroller.targetPosition = position
                        startSmoothScroll(smoothScroller)
                    }
                }

            layoutManager = customLayoutManager
            setHasFixedSize(true)
            // Изменяем анимацию добавления/удаления элементов списка (из библиотеки)
//            itemAnimator = OvershootInLeftAnimator()
        }
    }

    @Subscribe
    fun onMessageEvent(event: MessageEvent?) {
//        val test = event?.serviceFlag
//        if (test != null) {
//            isServiceTest = test
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView activity=${hashCode()}")
//        // Закрываем EventBus
//        EventBus.getDefault().unregister(this)

        // Обнуляем байдинг
        _binding = null
    }

    companion object {
        const val KEY_TEXT = "key_text"
        const val TAG = "AvtoInformatorFragment"

        fun newInstance(str: String): AvtoInformatorFragment {
            return AvtoInformatorFragment().withArguments {
//                putString(KEY_TEXT, str)
            }
        }
    }

    // При нажатии на остановку воспроизводим название
    private fun play(id: Int) {
        stopListViewModel.serviceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"play":[${id}]}""")
        )
    }

    // Меняем направление маршрута
    private fun toggle() {
        stopListViewModel.serviceManager?.sendMessage(
            ServiceManager.ServiceType.Informer,
            JSONObject("""{"dirrection":"toggle"}""")
        )
    }

    // Через интерфейс получаем событие о том что список изменился
    // Обновляем список и скролим до нужной остановки
    override fun updateList() {
        val ind = stopListViewModel.requestStops()
        busStopList.smoothScrollToPosition(ind)
    }

    // Принимаем сообщение от диспетчера
    override fun messageFromDispatcher(
        message: MutableState<String>,
        mPopupButton: SnapshotStateList<AvtoInformatorRepository.Answer>
    ) {
        Toast.makeText(requireContext(), "Пришло сообщение от диспетчера", Toast.LENGTH_SHORT)
            .show()

        // Создаем диалог
        lateinit var alertDialog: AlertDialog
        alertDialog = requireActivity().let {
            val myBuilder = AlertDialog.Builder(it)
            val layout = LinearLayout(it)
            layout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layout.orientation = LinearLayout.HORIZONTAL
            layout.gravity = Gravity.CENTER

            // Создаем кнопки ответа на сообщение
            mPopupButton.forEach { answer ->
                val button = Button(it)
                button.text = answer.mText
                button.setTextColor(resources.getColor(R.color.white))
                button.setBackgroundResource(R.drawable.button_square)
                button.setPadding(10)

                val customLayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                customLayoutParams.setMargins(10,10,10,10)

                button.layoutParams = customLayoutParams

                button.setOnClickListener {
                    stopListViewModel.answerQuery(answer.mText, answer.mTopic)
                    alertDialog.cancel()
                }

                layout.addView(button)
            }

            myBuilder.setTitle(message.component1())
                .setView(layout)
                .create()
        }

        alertDialog.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        alertDialog.show()
    }

    override fun reconnectService() {}
}