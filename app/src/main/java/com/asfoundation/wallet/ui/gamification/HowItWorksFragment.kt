package com.asfoundation.wallet.ui.gamification

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannedString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.asf.wallet.R
import com.asfoundation.wallet.analytics.gamification.GamificationAnalytics
import com.asfoundation.wallet.ui.iab.FiatValue
import dagger.android.support.DaggerFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_gamification_how_it_works.*
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class HowItWorksFragment : DaggerFragment(), HowItWorksView {
  @Inject
  lateinit var gamificationInteractor: GamificationInteractor
  @Inject
  lateinit var levelResourcesMapper: LevelResourcesMapper
  @Inject
  lateinit var analytics: GamificationAnalytics

  private lateinit var presenter: HowItWorksPresenter
  private lateinit var gamificationView: GamificationView

  private lateinit var bonusEarnedTextView: TextView
  private lateinit var totalSpendTextView: TextView
  private lateinit var nextLevelFooter: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    presenter = HowItWorksPresenter(this, gamificationInteractor, analytics, Schedulers.io(),
        AndroidSchedulers.mainThread())
  }


  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context !is GamificationView) {
      throw IllegalArgumentException(
          HowItWorksFragment::class.java.simpleName + " needs to be attached to a " + GamificationView::class.java.simpleName)
    }
    gamificationView = context
  }

  override fun showLevels(levels: List<ViewLevel>, currentLevel: Int) {
    fragment_gamification_how_it_works_loading.visibility = View.GONE
    var view: View?

    for (level in levels) {
      view = layoutInflater.inflate(R.layout.fragment_gamification_how_it_works_level,
          fragment_gamification_how_it_works_levels_layout, false)
      val levelTextView = view.findViewById<TextView>(R.id.level)
      val messageTextView = view.findViewById<TextView>(R.id.message)
      val bonusTextView = view.findViewById<TextView>(R.id.bonus)
      levelTextView.text = (level.level + 1).toString()
      messageTextView.text =
          getString(R.string.gamification_how_table_a2, formatLevelInfo(level.amount.toDouble()))
      bonusTextView.text =
          getString(R.string.gamification_how_table_b2, formatLevelInfo(level.bonus))
      view.findViewById<ImageView>(R.id.ic_level)
          .setImageResource(levelResourcesMapper.mapDarkIcons(level))
      (fragment_gamification_how_it_works_levels_layout as LinearLayout).addView(view)
      if (level.level == currentLevel) {
        highlightCurrentLevel(levelTextView, messageTextView, bonusTextView)
      }
    }
  }

  override fun showPeekInformation(totalSpend: BigDecimal, bonusEarned: FiatValue) {
    val totalSpendRounded = totalSpend.setScale(2, RoundingMode.DOWN)
    val bonusEarnedRounded = bonusEarned.amount.setScale(2, RoundingMode.DOWN)

    bonusEarnedTextView.text =
        getString(R.string.value_fiat, bonusEarned.symbol, bonusEarnedRounded)
    totalSpendTextView.text = getString(R.string.gamification_how_table_a2, totalSpendRounded)
  }

  override fun showNextLevelFooter(userStatus: UserRewardsStatus) {
    if (userStatus.level == 4) {
      nextLevelFooter.text = getString(R.string.gamification_how_max_level_body)
    } else {
      val nextLevel = (userStatus.level + 2).toString()
      nextLevelFooter.text =
          formatNextLevelFooter(R.string.gamification_how_to_next_level_body,
              userStatus.toNextLevelAmount.toString(), nextLevel)
    }
  }

  private fun formatNextLevelFooter(id: Int, nextLevelAmount: String,
                                    nextLevel: String): CharSequence {
    return HtmlCompat.fromHtml(String.format(
        HtmlCompat.toHtml(SpannedString(getText(id)), HtmlCompat.FROM_HTML_MODE_LEGACY),
        nextLevelAmount, nextLevel), HtmlCompat.FROM_HTML_MODE_LEGACY)
  }

  private fun formatLevelInfo(value: Double): String {
    val splitValue = value.toString()
        .split(".")
    return if (splitValue[1] != "0") {
      value.toString()
    } else {
      removeDecimalPlaces(value)
    }
  }

  private fun removeDecimalPlaces(value: Double): String {
    val splitValue = value.toString()
        .split(".")
    return splitValue[0]
  }

  private fun highlightCurrentLevel(levelTextView: TextView, messageTextView: TextView,
                                    bonusTextView: TextView) {
    val currentLevelColour = Color.parseColor("#001727")
    levelTextView.typeface = Typeface.DEFAULT_BOLD
    levelTextView.setTextColor(currentLevelColour)
    messageTextView.typeface = Typeface.DEFAULT_BOLD
    messageTextView.setTextColor(currentLevelColour)
    bonusTextView.typeface = Typeface.DEFAULT_BOLD
    bonusTextView.setTextColor(currentLevelColour)
  }

  override fun close() {
    gamificationView.closeHowItWorksView()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_gamification_how_it_works, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    bonusEarnedTextView = view.findViewById(R.id.bonus_earned)
    totalSpendTextView = view.findViewById(R.id.total_spend)
    nextLevelFooter = view.findViewById(R.id.next_level_footer)
    presenter.present(savedInstanceState)
  }

  override fun onDestroyView() {
    gamificationView.onHowItWorksClosed()
    presenter.stop()
    super.onDestroyView()
  }

  companion object {
    private val TAG = HowItWorksFragment::class.java.simpleName
    @JvmStatic
    fun newInstance(): HowItWorksFragment {
      return HowItWorksFragment()
    }
  }
}