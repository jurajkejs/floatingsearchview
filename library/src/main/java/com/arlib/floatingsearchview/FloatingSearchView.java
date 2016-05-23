/*
 * Copyright (C) 2015 Arlib
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arlib.floatingsearchview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.arlib.floatingsearchview.util.Util;
import com.arlib.floatingsearchview.util.adapter.GestureDetectorListenerAdapter;
import com.arlib.floatingsearchview.util.adapter.OnItemTouchListenerAdapter;
import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;
import com.arlib.floatingsearchview.util.view.MenuView;
import com.bartoszlipinski.viewpropertyobjectanimator.ViewPropertyObjectAnimator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A search UI widget that implements a floating search box also called persistent
 * search.
 */
public class FloatingSearchView extends FrameLayout {

    private static final String TAG = "FloatingSearchView";
    private static final int CARD_VIEW_SHADOW_HEIGHT = 3;
    private static final int CARD_VIEW_CORNERS_AND_TOP_BOTTOM_SHADOW_HEIGHT = 5;
    private static final long CLEAR_BTN_FADE_ANIM_DURATION = 500;
    private static final int CLEAR_BTN_WIDTH = 48;

    private final int BACKGROUND_DRAWABLE_ALPHA_SEARCH_ACTIVE = 150;
    private final int BACKGROUND_DRAWABLE_ALPHA_SEARCH_INACTIVE = 0;

    private final int MENU_ICON_ANIM_DURATION = 250;
    private final int BACKGROUND_FADE__ANIM_DURATION = 250;

    private final int ATTRS_SEARCH_BAR_MARGIN_DEFAULT = 0;

    /*
     * The ideal min width that the left icon plus the query EditText
     * should have. It applies only when determining how to render
     * the action items, it doesn't set the views' min attributes.
     */
    public final int SEARCH_BAR_LEFT_SECTION_DESIRED_WIDTH;

    public final static int LEFT_ACTION_MODE_SHOW_HAMBURGER_ENUM_VAL = 1;
    public final static int LEFT_ACTION_MODE_SHOW_SEARCH_ENUM_VAL = 2;
    public final static int LEFT_ACTION_MODE_SHOW_HOME_ENUM_VAL = 3;
    public final static int LEFT_ACTION_MODE_NO_LEFT_ACTION_ENUM_VAL = 4;

    @IntDef({LEFT_ACTION_MODE_SHOW_HAMBURGER_ENUM_VAL, LEFT_ACTION_MODE_SHOW_SEARCH_ENUM_VAL,
            LEFT_ACTION_MODE_SHOW_HOME_ENUM_VAL
            , LEFT_ACTION_MODE_NO_LEFT_ACTION_ENUM_VAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LeftActionMode {
    }

    @LeftActionMode
    private final int ATTRS_SEARCH_BAR_LEFT_ACTION_MODE_DEFAULT = LEFT_ACTION_MODE_NO_LEFT_ACTION_ENUM_VAL;

    private final boolean ATTRS_SHOW_MOVE_UP_SUGGESTION_DEFAULT = false;
    private final boolean ATTRS_DISMISS_ON_OUTSIDE_TOUCH_DEFAULT = false;
    private final boolean ATTRS_SEARCH_BAR_SHOW_SEARCH_KEY_DEFAULT = true;
    private final int ATTRS_SUGGESTION_TEXT_SIZE_SP_DEFAULT = 18;

    private final int SUGGEST_ITEM_ADD_ANIM_DURATION = 200;
    private final Interpolator SUGGEST_ITEM_ADD_ANIM_INTERPOLATOR = new LinearInterpolator();

    private Activity mHostActivity;

    private Drawable mBackgroundDrawable;
    private boolean mDismissOnOutsideTouch = true;

    private View mMainLayout;
    private CardView mQuerySection;
    private OnSearchListener mSearchListener;
    private boolean mIsFocused;
    private OnFocusChangeListener mFocusChangeListener;
    private EditText mSearchInput;
    private String mTitleText;
    private boolean mIsTitleSet;
    private int mSearchInputTextColor = -1;
    private int mSearchInputHintColor = -1;
    private View mSearchInputParent;
    private String mOldQuery = "";
    private OnQueryChangeListener mQueryListener;
    private ImageView mLeftAction;
    private OnLeftMenuClickListener mOnMenuClickListener;
    private OnHomeActionClickListener mOnHomeActionClickListener;
    private ProgressBar mSearchProgress;
    private DrawerArrowDrawable mMenuBtnDrawable;
    private Drawable mIconBackArrow;
    private Drawable mIconSearch;
    @LeftActionMode
    int mLeftActionMode;
    private boolean mSearchCanFocus;
    private String mSearchHint;
    private boolean mShowSearchKey;
    private boolean mMenuOpen = false;
    private MenuView mMenuView;
    private int mActionMenuItemColor;
    private int mOverflowIconColor;
    private OnMenuItemClickListener mActionMenuItemListener;
    private ImageView mClearButton;
    private Drawable mIconClear;
    private boolean mSkipQueryFocusChangeEvent;
    private boolean mSkipTextChangeEvent;

    private View mDivider;

    private RelativeLayout mSuggestionsSection;
    private View mSuggestionListContainer;
    private RecyclerView mSuggestionsList;
    private int mSuggestionTextColor = -1;
    private int mSuggestionRightIconColor;
    private SearchSuggestionsAdapter mSuggestionsAdapter;
    private SearchSuggestionsAdapter.OnBindSuggestionCallback mOnBindSuggestionCallback;
    private boolean mIsCollapsing = false;
    private int mSuggestionsTextSizePx;
    private boolean mIsInitialLayout = true;
    private boolean mIsSuggestionsSecHeightSet;
    private boolean mShowMoveUpSuggestion = ATTRS_SHOW_MOVE_UP_SUGGESTION_DEFAULT;

    //An interface for implementing a listener that will get notified when the suggestions
    //section's height is set. This is to be used internally only.
    private interface OnSuggestionSecHeightSetListener {
        void onSuggestionSecHeightSet();
    }

    private OnSuggestionSecHeightSetListener mSuggestionSecHeightListener;

    /**
     * Interface for implementing a listener to listen
     * to for state changes in the query text.
     */
    public interface OnQueryChangeListener {

        /**
         * Called when the query has changed. it will
         * be invoked when one or more characters in the
         * query was changed.
         *
         * @param oldQuery the previous query
         * @param newQuery the new query
         */
        void onSearchTextChanged(String oldQuery, String newQuery);
    }

    /**
     * Interface for implementing a listener to listen
     * to when the current search has completed.
     */
    public interface OnSearchListener {

        /**
         * Called when a suggestion was clicked indicating
         * that the current search has completed.
         *
         * @param searchSuggestion
         */
        void onSuggestionClicked(SearchSuggestion searchSuggestion);

        /**
         * Called when the current search has completed
         * as a result of pressing search key in the keyboard.
         * <p/>
         * Note: This will only get called if
         * {@link FloatingSearchView#setShowSearchKey(boolean)}} is set to true.
         *
         * @param currentQuery the text that is currently set in the query TextView
         */
        void onSearchAction(String currentQuery);
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the left menu (navigation menu) is
     * clicked. Notice that this listener only makes sense when
     * <p/>
     * Note: This is only relevant when leftActionMode is
     * set to {@value #LEFT_ACTION_MODE_SHOW_HAMBURGER_ENUM_VAL}
     */
    public interface OnLeftMenuClickListener {

        /**
         * Called when the menu button was
         * clicked and the menu's state is now opened.
         */
        void onMenuOpened();

        /**
         * Called when the back button was
         * clicked and the menu's state is now closed.
         */
        void onMenuClosed();
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the home action button (the back arrow)
     * is clicked.
     * <p/>
     * Note: This is only relevant when leftActionMode is
     * set to {@value #LEFT_ACTION_MODE_SHOW_HOME_ENUM_VAL}
     */
    public interface OnHomeActionClickListener {

        /**
         * Called when the home button was
         * clicked.
         */
        void onHomeClicked();
    }

    /**
     * Interface for implementing a listener to listen
     * when an item in the action (the item can be presented as an action
     * ,or as a menu item in the overflow menu) menu has been selected.
     */
    public interface OnMenuItemClickListener {

        /**
         * Called when a menu item in has been
         * selected.
         *
         * @param item the selected menu item.
         */
        void onActionMenuItemSelected(MenuItem item);
    }

    /**
     * Interface for implementing a listener to listen
     * to for focus state changes.
     */
    public interface OnFocusChangeListener {

        /**
         * Called when the search bar has gained focus
         * and listeners are now active.
         */
        void onFocus();

        /**
         * Called when the search bar has lost focus
         * and listeners are no more active.
         */
        void onFocusCleared();
    }

    public FloatingSearchView(Context context) {
        this(context, null);
    }

    public FloatingSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SEARCH_BAR_LEFT_SECTION_DESIRED_WIDTH = Util.dpToPx(225);
        init(attrs);
    }

    private void init(AttributeSet attrs) {

        mHostActivity = getHostActivity();

        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMainLayout = inflate(getContext(), R.layout.floating_search_layout, this);

        mBackgroundDrawable = new ColorDrawable(Color.BLACK);

        mQuerySection = (CardView) findViewById(R.id.search_query_section);
        mClearButton = (ImageView) findViewById(R.id.clear_btn);
        mSearchInput = (EditText) findViewById(R.id.search_bar_text);
        mSearchInputParent = findViewById(R.id.search_input_parent);
        mLeftAction = (ImageView) findViewById(R.id.left_action);
        mSearchProgress = (ProgressBar) findViewById(R.id.search_bar_search_progress);
        initDrawables();
        mClearButton.setImageDrawable(mIconClear);
        mMenuView = (MenuView) findViewById(R.id.menu_view);

        mDivider = findViewById(R.id.divider);

        mSuggestionsSection = (RelativeLayout) findViewById(R.id.search_suggestions_section);
        mSuggestionListContainer = findViewById(R.id.suggestions_list_container);
        mSuggestionsList = (RecyclerView) findViewById(R.id.suggestions_list);

        setupViews(attrs);
    }

    private Activity getHostActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private void initDrawables() {
        mMenuBtnDrawable = new DrawerArrowDrawable(getContext());
        mIconClear = Util.getWrappedDrawable(getContext(), R.drawable.ic_clear_black_24dp);
        mIconBackArrow = Util.getWrappedDrawable(getContext(), R.drawable.ic_arrow_back_black_24dp);
        mIconSearch = Util.getWrappedDrawable(getContext(), R.drawable.ic_search_black_24dp);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (mIsInitialLayout) {

            //we need to add 5dp to the mSuggestionsSection because we are
            //going to move it up by 5dp in order to cover the search bar's
            //rounded corners. We also need to add an additional 10dp to
            //mSuggestionsSection in order to hide mSuggestionListContainer
            //rounded corners and top/bottom padding.
            int addedHeight = 3 * Util.dpToPx(CARD_VIEW_CORNERS_AND_TOP_BOTTOM_SHADOW_HEIGHT);
            final int finalHeight = mSuggestionsSection.getHeight() + addedHeight;
            mSuggestionsSection.getLayoutParams().height = finalHeight;
            mSuggestionsSection.requestLayout();
            ViewTreeObserver vto = mSuggestionListContainer.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    if (mSuggestionsSection.getHeight() == finalHeight) {
                        Util.removeGlobalLayoutObserver(mSuggestionListContainer, this);

                        mIsSuggestionsSecHeightSet = true;
                        if (mSuggestionSecHeightListener != null) {
                            mSuggestionSecHeightListener.onSuggestionSecHeightSet();
                            mSuggestionSecHeightListener = null;
                        }

                        moveSuggestListToInitialPos();
                    }
                }
            });

            mIsInitialLayout = false;
        }
    }

    private void setupViews(AttributeSet attrs) {

        if (attrs != null)
            applyXmlAttributes(attrs);

        mBackgroundDrawable.setAlpha(BACKGROUND_DRAWABLE_ALPHA_SEARCH_INACTIVE);

        int sdkVersion = Build.VERSION.SDK_INT;
        if (sdkVersion < Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(mBackgroundDrawable);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(mBackgroundDrawable);
        }

        setupQueryBar();

        if (!isInEditMode()) {
            setupSuggestionSection();
        }
    }

    private void applyXmlAttributes(AttributeSet attrs) {

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FloatingSearchView);

        try {

            setDismissOnOutsideClick(true);

            int searchBarWidth = a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchBarWidth,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            mQuerySection.getLayoutParams().width = searchBarWidth;
            mDivider.getLayoutParams().width = searchBarWidth;
            mSuggestionListContainer.getLayoutParams().width = searchBarWidth;

            int searchBarLeftMargin = a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchBarMarginLeft,
                    ATTRS_SEARCH_BAR_MARGIN_DEFAULT);
            int searchBarTopMargin = a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchBarMarginTop,
                    ATTRS_SEARCH_BAR_MARGIN_DEFAULT);
            int searchBarRightMargin = a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchBarMarginRight,
                    ATTRS_SEARCH_BAR_MARGIN_DEFAULT);
            LayoutParams querySectionLP = (LayoutParams) mQuerySection.getLayoutParams();
            LayoutParams dividerLP = (LayoutParams) mDivider.getLayoutParams();
            LinearLayout.LayoutParams suggestListSectionLP =
                    (LinearLayout.LayoutParams) mSuggestionsSection.getLayoutParams();
            int cardPadding = Util.dpToPx(3);
            querySectionLP.setMargins(searchBarLeftMargin, searchBarTopMargin,
                    searchBarRightMargin, 0);
            dividerLP.setMargins(searchBarLeftMargin + cardPadding, 0,
                    searchBarRightMargin + cardPadding,
                    ((MarginLayoutParams) mDivider.getLayoutParams()).bottomMargin);
            suggestListSectionLP.setMargins(searchBarLeftMargin, 0, searchBarRightMargin, 0);
            mQuerySection.setLayoutParams(querySectionLP);
            mDivider.setLayoutParams(dividerLP);
            mSuggestionsSection.setLayoutParams(suggestListSectionLP);

            setSearchHint(a.getString(R.styleable.FloatingSearchView_floatingSearch_searchHint));
            setShowSearchKey(a.getBoolean(R.styleable.FloatingSearchView_floatingSearch_showSearchKey,
                    ATTRS_SEARCH_BAR_SHOW_SEARCH_KEY_DEFAULT));
            setDismissOnOutsideClick(a.getBoolean(R.styleable.FloatingSearchView_floatingSearch_dismissOnOutsideTouch,
                    ATTRS_DISMISS_ON_OUTSIDE_TOUCH_DEFAULT));
            setSuggestionItemTextSize(a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchSuggestionTextSize,
                    Util.spToPx(ATTRS_SUGGESTION_TEXT_SIZE_SP_DEFAULT)));
            setLeftActionMode(a.getInt(R.styleable.FloatingSearchView_floatingSearch_leftActionMode,
                    LEFT_ACTION_MODE_NO_LEFT_ACTION_ENUM_VAL));
            if (mLeftActionMode == LEFT_ACTION_MODE_NO_LEFT_ACTION_ENUM_VAL) {
                mSearchInputParent.setTranslationX(-Util.dpToPx(48 + 20 - 16));
            }
            if (a.hasValue(R.styleable.FloatingSearchView_floatingSearch_menu)) {
                mMenuView.resetMenuResource(a.getResourceId(R.styleable.FloatingSearchView_floatingSearch_menu, 0));
            }
            setShowMoveUpSuggestion(a.getBoolean(R.styleable.FloatingSearchView_floatingSearch_show_move_suggestion_up,
                    ATTRS_SHOW_MOVE_UP_SUGGESTION_DEFAULT));

            setBackgroundColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_backgroundColor
                    , Util.getColor(getContext(), R.color.background)));
            setLeftActionIconColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_leftActionColor
                    , Util.getColor(getContext(), R.color.left_action_icon)));
            setActionMenuOverflowColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_actionMenuOverflowColor
                    , Util.getColor(getContext(), R.color.overflow_icon_color)));
            setMenuItemIconColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_menuItemIconColor
                    , Util.getColor(getContext(), R.color.menu_icon_color)));
            setDividerColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_dividerColor
                    , Util.getColor(getContext(), R.color.divider)));
            setClearBtnColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_clearBtnColor
                    , Util.getColor(getContext(), R.color.clear_btn_color)));
            setViewTextColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_viewTextColor
                    , Util.getColor(getContext(), R.color.dark_gray)));
            setHintTextColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_hintTextColor
                    , Util.getColor(getContext(), R.color.hint_color)));
            setSuggestionRightIconColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_suggestionRightIconColor
                    , Util.getColor(getContext(), R.color.gray_active_icon)));
        } finally {
            a.recycle();
        }
    }

    private void setupQueryBar() {

        mSearchInput.setTextColor(this.mSearchInputTextColor);
        mSearchInput.setHintTextColor(this.mSearchInputHintColor);

        if (!isInEditMode() && mHostActivity != null) {
            mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }

        if (isInEditMode()) {
            mMenuView.reset(actionMenuAvailWidth());
        }

        ViewTreeObserver vto = mQuerySection.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Util.removeGlobalLayoutObserver(mQuerySection, this);

                mMenuView.reset(actionMenuAvailWidth());
                if (mIsFocused) {
                    mMenuView.hideIfRoomItems(false);
                }
            }
        });

        mMenuView.setMenuCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {

                if (mActionMenuItemListener != null) {
                    mActionMenuItemListener.onActionMenuItemSelected(item);
                }

                //todo check if we should care about this return or not
                return false;
            }

            @Override
            public void onMenuModeChange(MenuBuilder menu) {
            }

        });

        mMenuView.setOnVisibleWidthChanged(new MenuView.OnVisibleWidthChanged() {
            @Override
            public void onItemsMenuVisibleWidthChanged(int newVisibleWidth) {

                //todo remove magic numbers
                if (newVisibleWidth == 0) {
                    mClearButton.setTranslationX(-Util.dpToPx(4));
                    int paddingRight = newVisibleWidth + Util.dpToPx(4);
                    if (mIsFocused) {
                        paddingRight += Util.dpToPx(CLEAR_BTN_WIDTH);
                    }
                    mSearchInput.setPadding(0, 0, paddingRight, 0);
                } else {
                    mClearButton.setTranslationX(-newVisibleWidth);
                    int paddingRight = newVisibleWidth;
                    if (mIsFocused) {
                        paddingRight += Util.dpToPx(CLEAR_BTN_WIDTH);
                    }
                    mSearchInput.setPadding(0, 0, paddingRight, 0);
                }
            }
        });

        mMenuView.setActionIconColor(this.mActionMenuItemColor);
        mMenuView.setOverflowColor(this.mOverflowIconColor);

        mClearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mSearchInput.setText("");
            }
        });

        mClearButton.setVisibility(View.INVISIBLE);
        mSearchInput.addTextChangedListener(new TextWatcherAdapter() {

            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                //todo investigate why this is called twice when pressing back on the keyboard

                if (mSkipTextChangeEvent || !mIsFocused) {
                    mSkipTextChangeEvent = false;
                } else {
                    if (mSearchInput.getText().toString().length() != 0 &&
                            mClearButton.getVisibility() == View.INVISIBLE) {
                        mClearButton.setAlpha(0.0f);
                        mClearButton.setVisibility(View.VISIBLE);
                        ViewCompat.animate(mClearButton).alpha(1.0f).setDuration(CLEAR_BTN_FADE_ANIM_DURATION).start();
                    } else if (mSearchInput.getText().toString().length() == 0) {
                        mClearButton.setVisibility(View.INVISIBLE);
                    }

                    if (mQueryListener != null && mIsFocused && !mOldQuery.equals(mSearchInput.getText().toString())) {
                        mQueryListener.onSearchTextChanged(mOldQuery, mSearchInput.getText().toString());
                    }

                    mOldQuery = mSearchInput.getText().toString();
                }
            }

        });

        mSearchInput.setOnFocusChangeListener(new TextView.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (mSkipQueryFocusChangeEvent) {
                    mSkipQueryFocusChangeEvent = false;
                } else if (hasFocus != mIsFocused) {
                    setSearchFocusedInternal(hasFocus);
                }
            }
        });

        mSearchInput.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {

                if (mShowSearchKey && keyCode == KeyEvent.KEYCODE_ENTER) {
                    setSearchFocusedInternal(false);
                    if (mSearchListener != null) {
                        mSearchListener.onSearchAction(getQuery());
                    }
                    return true;
                }
                return false;
            }
        });

        mLeftAction.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isSearchBarFocused()) {
                    setSearchFocusedInternal(false);
                } else {
                    switch (mLeftActionMode) {
                        case LEFT_ACTION_MODE_SHOW_HAMBURGER_ENUM_VAL:
                            toggleLeftMenu();
                            break;
                        case LEFT_ACTION_MODE_SHOW_SEARCH_ENUM_VAL:
                            setSearchFocusedInternal(true);
                            break;
                        case LEFT_ACTION_MODE_SHOW_HOME_ENUM_VAL:
                            if (mOnHomeActionClickListener != null) {
                                mOnHomeActionClickListener.onHomeClicked();
                            }
                            break;
                        case LEFT_ACTION_MODE_NO_LEFT_ACTION_ENUM_VAL:
                            //do nothing
                            break;
                    }
                }

            }
        });

        refreshLeftIcon();
    }

    private int actionMenuAvailWidth() {
        if (isInEditMode()) {
            return Util.dpToPx(360) - SEARCH_BAR_LEFT_SECTION_DESIRED_WIDTH;
        }
        return mQuerySection.getWidth() - SEARCH_BAR_LEFT_SECTION_DESIRED_WIDTH;
    }

    /**
     * Sets the menu button's color.
     *
     * @param color the color to be applied to the
     *              left menu button.
     */
    public void setLeftActionIconColor(int color) {
        mMenuBtnDrawable.setColor(color);
        DrawableCompat.setTint(mIconBackArrow, color);
        DrawableCompat.setTint(mIconSearch, color);
    }

    /**
     * Sets the clear button's color.
     *
     * @param color the color to be applied to the
     *              clear button.
     */
    public void setClearBtnColor(int color) {
        DrawableCompat.setTint(mIconClear, color);
    }

    /**
     * Sets the action menu icons' color.
     *
     * @param color the color to be applied to the
     *              action menu items.
     */
    public void setMenuItemIconColor(int color) {
        this.mActionMenuItemColor = color;
        if (mMenuView != null) {
            mMenuView.setActionIconColor(this.mActionMenuItemColor);
        }
    }

    /**
     * Sets the action menu overflow icon's color.
     *
     * @param color the color to be applied to the
     *              overflow icon.
     */
    public void setActionMenuOverflowColor(int color) {
        this.mOverflowIconColor = color;
        if (mMenuView != null) {
            mMenuView.setOverflowColor(this.mOverflowIconColor);
        }
    }

    /**
     * Sets the background color of the search
     * view including the suggestions section.
     *
     * @param color the color to be applied to the search bar and
     *              the suggestion section background.
     */
    public void setBackgroundColor(int color) {
        if (mQuerySection != null && mSuggestionsList != null) {
            mQuerySection.setCardBackgroundColor(color);
            mSuggestionsList.setBackgroundColor(color);
        }
    }

    //TODO

    /**
     * Sets the text color of the search
     * and suggestion text.
     *
     * @param color the color to be applied to the search and suggestion
     *              text.
     */
    public void setViewTextColor(int color) {
        this.mSearchInputTextColor = color;
        this.mSuggestionTextColor = color;
        if (mSearchInput != null && mSuggestionsAdapter != null) {
            mSearchInput.setTextColor(color);
            mSuggestionsAdapter.setTextColor(color);
        }
    }

    /**
     * Sets the text color of the search
     * hint.
     *
     * @param color the color to be applied to the search hint.
     */
    public void setHintTextColor(int color) {
        this.mSearchInputHintColor = color;
        if (mSearchInput != null) {
            mSearchInput.setHintTextColor(color);
        }
    }

    /**
     * Sets the color of the search divider that
     * divides the search section from the suggestions.
     *
     * @param color the color to be applied the divider.
     */
    public void setDividerColor(int color) {
        if (mDivider != null) {
            mDivider.setBackgroundColor(color);
        }
    }

    /**
     * Set the tint of the suggestion items' right btn (move suggestion to
     * query input btn)
     *
     * @param color
     */
    public void setSuggestionRightIconColor(int color) {
        this.mSuggestionRightIconColor = color;

        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter.setRightIconColor(this.mSuggestionRightIconColor);
        }
    }

    /**
     * Set the text size of the suggestion items.
     * TODO
     *
     * @param sizePx
     */
    private void setSuggestionItemTextSize(int sizePx) {
        this.mSuggestionsTextSizePx = sizePx;
        //setup adapter and make method public
    }

    /**
     * Set the mode for the left action button.
     *
     * @param mode
     */
    private void setLeftActionMode(int mode) {

        //todo implement dynamic leftActionMode setting and expose method
        mLeftActionMode = mode;
        //refreshLeftIcon();
    }

    /**
     * Inflates the menu items from
     * an xml resource.
     *
     * @param menuId a menu xml resource reference
     */
    public void inflateOverflowMenu(int menuId) {
        mMenuView.resetMenuResource(menuId);

        //todo check fo synchronization problems in MenuView
        //when calling it this way
        mMenuView.reset(actionMenuAvailWidth());
        if (mIsFocused)
            mMenuView.hideIfRoomItems(false);
    }

    /**
     * Set a hint that will appear in the
     * search input. Default hint is R.string.abc_search_hint
     * which is "search..." (when device language is set to english)
     *
     * @param searchHint
     */
    public void setSearchHint(String searchHint) {
        mSearchHint = searchHint != null ? searchHint : getResources().getString(R.string.abc_search_hint);
        mSearchInput.setHint(mSearchHint);
    }

    /**
     * Sets whether the the button with the search icon
     * will appear in the soft-keyboard or not.
     *
     * @param show to show the search button in
     *             the soft-keyboard.
     */
    public void setShowSearchKey(boolean show) {
        mShowSearchKey = show;
        if (show) {
            mSearchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        } else {
            mSearchInput.setImeOptions(EditorInfo.IME_ACTION_NONE);
        }
    }

    /**
     * Set whether a touch outside of the
     * search bar's bounds will cause the search bar to
     * loos focus.
     *
     * @param enable true to dismiss on outside touch, false otherwise.
     */
    public void setDismissOnOutsideClick(boolean enable) {

        mDismissOnOutsideTouch = enable;
        mSuggestionsSection.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //todo check if this is called twice
                if (mDismissOnOutsideTouch && mIsFocused) {
                    setSearchFocusedInternal(false);
                }

                return true;
            }
        });
    }

    /**
     * Sets the arrow up of suggestion items to be enabled and visible or
     * disabled and invisible.
     *
     * @param show
     */
    public void setShowMoveUpSuggestion(boolean show) {
        if (mSuggestionsAdapter != null) {
            mShowMoveUpSuggestion = show;
            mSuggestionsAdapter.setShowMoveUpIcon(mShowMoveUpSuggestion);
        }
    }

    /**
     * Wrapper implementation for EditText.setFocusable(boolean focusable)
     *
     * @param focusable true, to make search focus when
     *                  clicked.
     */
    public void setSearchFocusable(boolean focusable) {
        mSearchInput.setFocusable(focusable);
    }

    /**
     * Sets the title for the search bar.
     * <p/>
     * Note that after the title is set, when
     * the search gains focus, the title will be replaced
     * by the search hint.
     *
     * @param title the title to be shown when search
     *              is not focused
     */
    public void setSearchBarTitle(CharSequence title) {
        this.mTitleText = title.toString();
        mIsTitleSet = true;
        mSearchInput.setText(title);
    }

    /**
     * Sets the search text.
     * <p/>
     * Note that this is the different from
     * {@link #setSearchBarTitle(CharSequence title) setSearchBarTitle} in
     * that it keeps the text when the search gains focus.
     *
     * @param text the text to be set for the search
     *             input.
     */
    public void setSearchText(CharSequence text) {
        mIsTitleSet = false;
        mSearchInput.setText(text);
    }

    /**
     * Returns the current query text.
     *
     * @return the current query
     */
    public String getQuery() {
        return mSearchInput.getText().toString();
    }

    public void clearQuery() {
        mSearchInput.setText("");
    }

    /**
     * <p/>
     * Enables clients to directly manipulate
     * the menu icon's progress.
     * <p/>
     * Useful for custom animation/behaviors.
     *
     * @param progress the desired progress of the menu
     *                 icon's rotation. 0.0 == hamburger
     *                 shape, 1.0 == back arrow shape
     */
    public void setMenuIconProgress(float progress) {

        mMenuBtnDrawable.setProgress(progress);
        if (progress == 0) {
            closeMenu(false);
        } else if (progress == 1.0) {
            openMenu(false);
        }
    }

    private void refreshLeftIcon() {

        mLeftAction.setVisibility(VISIBLE);
        switch (mLeftActionMode) {
            case LEFT_ACTION_MODE_SHOW_HAMBURGER_ENUM_VAL:
                mLeftAction.setImageDrawable(mMenuBtnDrawable);
                break;
            case LEFT_ACTION_MODE_SHOW_SEARCH_ENUM_VAL:
                mLeftAction.setImageDrawable(mIconSearch);
                break;
            case LEFT_ACTION_MODE_SHOW_HOME_ENUM_VAL:
                mLeftAction.setImageDrawable(mMenuBtnDrawable);
                mMenuBtnDrawable.setProgress(1.0f);
                break;
            case LEFT_ACTION_MODE_NO_LEFT_ACTION_ENUM_VAL:
                mLeftAction.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void toggleLeftMenu() {
        if (mMenuOpen) {
            closeMenu(true);
        } else {
            openMenu(true);
        }
    }

    /**
     * Mimics a menu click that opens the menu. Useful when for navigation
     * drawers when they open as a result of dragging.
     */
    public void openMenu(boolean withAnim) {

        if (mOnMenuClickListener != null) {
            mOnMenuClickListener.onMenuOpened();
        }
        mMenuOpen = true;
        openMenuDrawable(mMenuBtnDrawable, withAnim);
    }

    /**
     * Set the hamburger menu to open or closed without calling
     * listeners.
     *
     * @param isOpen
     */
    public void setLeftMenuOpen(boolean isOpen) {
        mMenuOpen = isOpen;
        mMenuBtnDrawable.setProgress(isOpen ? 1.0f : 0.0f);
    }

    /**
     * Mimics a menu click that closes. Useful when fo navigation
     * drawers when they close as a result of selecting and item.
     *
     * @param withAnim true, will close the menu button with
     *                 the  Material animation
     */
    public void closeMenu(boolean withAnim) {
        if (mOnMenuClickListener != null) {
            mOnMenuClickListener.onMenuClosed();
        }
        mMenuOpen = false;
        closeMenuDrawable(mMenuBtnDrawable, withAnim);
    }

    /**
     * Shows a circular progress on top of the
     * menu action button.
     * <p/>
     * Call hidProgress()
     * to change back to normal and make the menu
     * action visible.
     */
    public void showProgress() {
        mLeftAction.setVisibility(View.GONE);
        mSearchProgress.setAlpha(0.0f);
        mSearchProgress.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(mSearchProgress, "alpha", 0.0f, 1.0f).start();
    }

    /**
     * Hides the progress bar after
     * a prior call to showProgress()
     */
    public void hideProgress() {
        mSearchProgress.setVisibility(View.GONE);
        mLeftAction.setAlpha(0.0f);
        mLeftAction.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(mLeftAction, "alpha", 0.0f, 1.0f).start();
    }

    /**
     * Sets whether the search is focused or not.
     *
     * @param focused true, to set the search to be active/focused.
     */
    public void setSearchFocused(boolean focused) {

        if (!this.mIsFocused && mSuggestionSecHeightListener == null) {
            if (mIsSuggestionsSecHeightSet) {
                setSearchFocusedInternal(true);
            } else {
                mSuggestionSecHeightListener = new OnSuggestionSecHeightSetListener() {
                    @Override
                    public void onSuggestionSecHeightSet() {
                        setSearchFocusedInternal(true);
                        mSuggestionSecHeightListener = null;
                    }
                };
            }
        }
    }

    private void setupSuggestionSection() {

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, true);
        mSuggestionsList.setLayoutManager(layoutManager);
        mSuggestionsList.setItemAnimator(null);

        final GestureDetector gestureDetector = new GestureDetector(getContext(),
                new GestureDetectorListenerAdapter() {

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        if (mHostActivity != null) {
                            Util.closeSoftKeyboard(mHostActivity);
                        }
                        return false;
                    }
                });
        mSuggestionsList.addOnItemTouchListener(new OnItemTouchListenerAdapter() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                gestureDetector.onTouchEvent(e);
                return false;
            }
        });

        mSuggestionsAdapter = new SearchSuggestionsAdapter(getContext(), mSuggestionsTextSizePx,
                new SearchSuggestionsAdapter.Listener() {

                    @Override
                    public void onItemSelected(SearchSuggestion item) {

                        mSkipTextChangeEvent = true;
                        mSearchInput.setText(item.getBody());

                        setSearchFocusedInternal(false);

                        if (mSearchListener != null) {
                            mSearchListener.onSuggestionClicked(item);
                        }
                    }

                    @Override
                    public void onMoveItemToSearchClicked(SearchSuggestion item) {

                        mSearchInput.setText(item.getBody());

                        //move cursor to end of text
                        mSearchInput.setSelection(mSearchInput.getText().length());
                    }
                });
        mSuggestionsAdapter.setTextColor(this.mSuggestionTextColor);
        mSuggestionsAdapter.setRightIconColor(this.mSuggestionRightIconColor);

        mSuggestionsList.setAdapter(mSuggestionsAdapter);

        int cardViewBottomPadding = Util.dpToPx(CARD_VIEW_CORNERS_AND_TOP_BOTTOM_SHADOW_HEIGHT);
        //move up the suggestions section enough to cover the search bar
        //card's bottom left and right corners
        mSuggestionsSection.setTranslationY(-cardViewBottomPadding);
    }

    private void moveSuggestListToInitialPos() {

        //move the suggestions list to the collapsed position
        //which is translationY of -listHeight
        mSuggestionListContainer.setTranslationY(-mSuggestionListContainer.getHeight());
    }

    /**
     * Clears the current suggestions and replaces it
     * with the provided list of new suggestions.
     *
     * @param newSearchSuggestions a list containing the new suggestions
     */
    public void swapSuggestions(final List<? extends SearchSuggestion> newSearchSuggestions) {
        Collections.reverse(newSearchSuggestions);
        swapSuggestions(newSearchSuggestions, true);
    }

    private void swapSuggestions(final List<? extends SearchSuggestion> newSearchSuggestions,
                                 final boolean withAnim) {

        mSuggestionsAdapter.swapData(newSearchSuggestions);
        mSuggestionsList.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Util.removeGlobalLayoutObserver(mSuggestionsList, this);
                updateSuggestionsSectionHeight(newSearchSuggestions, withAnim);
            }
        });

        mDivider.setVisibility(!newSearchSuggestions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateSuggestionsSectionHeight(List<? extends SearchSuggestion>
                                                        newSearchSuggestions, boolean withAnim) {

        int cardTopBottomShadowPadding = Util.dpToPx(CARD_VIEW_CORNERS_AND_TOP_BOTTOM_SHADOW_HEIGHT);
        int cardRadiusSize = Util.dpToPx(CARD_VIEW_SHADOW_HEIGHT);

        //TODO GO OVER
        int visibleHeight = getVisibleItemsHeight(newSearchSuggestions);
        int diff = mSuggestionListContainer.getHeight() - visibleHeight;
        int addedTranslationYForShadowOffsets = diff <= cardTopBottomShadowPadding ?
                -(cardTopBottomShadowPadding - diff) :
                Math.max(cardRadiusSize - (diff - cardTopBottomShadowPadding), cardRadiusSize);
        final float newTranslationY = -mSuggestionListContainer.getHeight() +
                getVisibleItemsHeight(newSearchSuggestions) + addedTranslationYForShadowOffsets;

        final boolean animateAtEnd = newTranslationY >= mSuggestionListContainer.getTranslationY();

        ViewCompat.animate(mSuggestionListContainer).cancel();
        if (withAnim) {
            ViewCompat.animate(mSuggestionListContainer).
                    setInterpolator(SUGGEST_ITEM_ADD_ANIM_INTERPOLATOR).
                    setDuration(SUGGEST_ITEM_ADD_ANIM_DURATION).
                    translationY(newTranslationY).
                    setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationCancel(View view) {
                            mSuggestionListContainer.setTranslationY(newTranslationY);
                        }

                        @Override
                        public void onAnimationStart(View view) {
                            if (!animateAtEnd) {
                                mSuggestionsList.smoothScrollToPosition(0);
                            }
                        }

                        @Override
                        public void onAnimationEnd(View view) {
                            if (animateAtEnd) {
                                int lastPos = mSuggestionsList.getAdapter().getItemCount() - 1;
                                if (lastPos > -1) {
                                    mSuggestionsList.smoothScrollToPosition(lastPos);
                                }
                            }
                        }
                    }).start();
        } else {
            mSuggestionListContainer.setTranslationY(newTranslationY);
        }
    }

    //returns the cumulative height that the current suggestion items take up, or the full height
    //of the suggestions list, if the cumulative items' height is >= the lists height
    private int getVisibleItemsHeight(List<? extends SearchSuggestion> suggestions) {

        int visibleItemsHeight = 0;
        for (int i = 0; i < suggestions.size() && i < mSuggestionsList.getChildCount(); i++) {
            visibleItemsHeight += mSuggestionsList.getChildAt(i).getHeight();

            if (visibleItemsHeight > mSuggestionListContainer.getHeight()) {
                visibleItemsHeight = mSuggestionListContainer.getHeight();
                break;
            }
        }
        return visibleItemsHeight;
    }

    /**
     * Set a callback that will be called after each suggestion view in the suggestions recycler
     * list is bound. This allows for customized binding for specific items in the list.
     *
     * @param callback A callback to be called after a suggestion is bound by the suggestions list's
     *                 adapter.
     */
    public void setOnBindSuggestionCallback(SearchSuggestionsAdapter.OnBindSuggestionCallback callback) {
        this.mOnBindSuggestionCallback = callback;
        if (mSuggestionsAdapter != null)
            mSuggestionsAdapter.setOnBindSuggestionCallback(mOnBindSuggestionCallback);
    }

    /**
     * Collapses the suggestions list and
     * then clears its suggestion items.
     */
    public void clearSuggestions() {
        swapSuggestions(new ArrayList<SearchSuggestion>());
    }

    public void clearSearchFocus() {
        setSearchFocusedInternal(false);
    }

    public boolean isSearchBarFocused() {
        return mIsFocused;
    }

    private void setSearchFocusedInternal(boolean focused) {
        this.mIsFocused = focused;

        if (focused) {
            mSearchInput.requestFocus();
            moveSuggestListToInitialPos();
            fadeInBackground();
            mMenuView.hideIfRoomItems(true);
            transitionInLeftSection(true);
            Util.showSoftKeyboard(getContext(), mSearchInput);
            if (mMenuOpen) {
                closeMenu(false);
            }
            if (mIsTitleSet) {
                mSkipTextChangeEvent = true;
                mSearchInput.setText("");
            }
            if (mFocusChangeListener != null) {
                mFocusChangeListener.onFocus();
            }
        } else {
            mMainLayout.requestFocus();
            clearSuggestions();
            fadeOutBackground();
            mMenuView.showIfRoomItems(true);
            transitionOutLeftSection(true);
            mClearButton.setVisibility(View.GONE);
            if (mHostActivity != null) {
                Util.closeSoftKeyboard(mHostActivity);
            }
            if (mIsTitleSet) {
                mSkipTextChangeEvent = true;
                mSearchInput.setText(mTitleText);
            }
            if (mFocusChangeListener != null) {
                mFocusChangeListener.onFocusCleared();
            }
        }
    }

    private void changeIcon(ImageView imageView, Drawable newIcon, boolean withAnim) {
        imageView.setImageDrawable(newIcon);
        if (withAnim) {
            ObjectAnimator fadeInVoiceInputOrClear = ObjectAnimator.ofFloat(imageView, "alpha", 0.0f, 1.0f);
            fadeInVoiceInputOrClear.start();
        } else {
            imageView.setAlpha(1.0f);
        }
    }

    private void transitionInLeftSection(boolean withAnim) {
        mLeftAction.setVisibility(View.VISIBLE);

        switch (mLeftActionMode) {
            case LEFT_ACTION_MODE_SHOW_HAMBURGER_ENUM_VAL:
                openMenuDrawable(mMenuBtnDrawable, withAnim);
                if (!mMenuOpen) {
                    break;
                }
                break;
            case LEFT_ACTION_MODE_SHOW_SEARCH_ENUM_VAL:
                mLeftAction.setImageDrawable(mIconBackArrow);
                if (withAnim) {
                    mLeftAction.setRotation(45);
                    mLeftAction.setAlpha(0.0f);
                    ObjectAnimator rotateAnim = ViewPropertyObjectAnimator.animate(mLeftAction).rotation(0).get();
                    ObjectAnimator fadeAnim = ViewPropertyObjectAnimator.animate(mLeftAction).alpha(1.0f).get();
                    AnimatorSet animSet = new AnimatorSet();
                    animSet.setDuration(500);
                    animSet.playTogether(rotateAnim, fadeAnim);
                    animSet.start();
                }
                break;
            case LEFT_ACTION_MODE_SHOW_HOME_ENUM_VAL:
                //do nothing
                break;
            case LEFT_ACTION_MODE_NO_LEFT_ACTION_ENUM_VAL:
                mLeftAction.setImageDrawable(mIconBackArrow);

                if (withAnim) {
                    ObjectAnimator searchInputTransXAnim = ViewPropertyObjectAnimator.animate(mSearchInputParent).translationX(0).get();

                    mLeftAction.setScaleX(0.5f);
                    mLeftAction.setScaleY(0.5f);
                    mLeftAction.setAlpha(0.0f);
                    mLeftAction.setTranslationX(Util.dpToPx(8));
                    ObjectAnimator transXArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).translationX(1.0f).get();
                    ObjectAnimator scaleXArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleX(1.0f).get();
                    ObjectAnimator scaleYArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleY(1.0f).get();
                    ObjectAnimator fadeArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).alpha(1.0f).get();
                    transXArrowAnim.setStartDelay(150);
                    scaleXArrowAnim.setStartDelay(150);
                    scaleYArrowAnim.setStartDelay(150);
                    fadeArrowAnim.setStartDelay(150);

                    AnimatorSet animSet = new AnimatorSet();
                    animSet.setDuration(500);
                    animSet.playTogether(searchInputTransXAnim, transXArrowAnim, scaleXArrowAnim, scaleYArrowAnim, fadeArrowAnim);
                    animSet.start();
                } else {
                    mSearchInputParent.setTranslationX(0);
                }
                break;
        }
    }

    private void transitionOutLeftSection(boolean withAnim) {

        switch (mLeftActionMode) {
            case LEFT_ACTION_MODE_SHOW_HAMBURGER_ENUM_VAL:
                closeMenuDrawable(mMenuBtnDrawable, withAnim);
                break;
            case LEFT_ACTION_MODE_SHOW_SEARCH_ENUM_VAL:
                changeIcon(mLeftAction, mIconSearch, withAnim);
                break;
            case LEFT_ACTION_MODE_SHOW_HOME_ENUM_VAL:
                //do nothing
                break;
            case LEFT_ACTION_MODE_NO_LEFT_ACTION_ENUM_VAL:
                mLeftAction.setImageDrawable(mIconBackArrow);

                if (withAnim) {
                    ObjectAnimator searchInputTransXAnim = ViewPropertyObjectAnimator.animate(mSearchInputParent)
                            .translationX(-Util.dpToPx(48 + 20 - 16)).get();

                    ObjectAnimator scaleXArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleX(0.5f).get();
                    ObjectAnimator scaleYArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleY(0.5f).get();
                    ObjectAnimator fadeArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).alpha(0.5f).get();
                    scaleXArrowAnim.setDuration(300);
                    scaleYArrowAnim.setDuration(300);
                    fadeArrowAnim.setDuration(300);
                    scaleXArrowAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {

                            //restore normal state
                            mLeftAction.setScaleX(1.0f);
                            mLeftAction.setScaleY(1.0f);
                            mLeftAction.setAlpha(1.0f);
                            mLeftAction.setVisibility(View.INVISIBLE);
                        }
                    });

                    AnimatorSet animSet = new AnimatorSet();
                    animSet.setDuration(350);
                    animSet.playTogether(scaleXArrowAnim, scaleYArrowAnim, fadeArrowAnim, searchInputTransXAnim);
                    animSet.start();
                } else {
                    mLeftAction.setVisibility(View.INVISIBLE);
                }
                break;
        }
    }

    /**
     * Sets the listener that will listen for query
     * changes as they are being typed.
     *
     * @param listener listener for query changes
     */
    public void setOnQueryChangeListener(OnQueryChangeListener listener) {
        this.mQueryListener = listener;
    }

    /**
     * Sets the listener that will be called when
     * an action that completes the current search
     * session has occurred and the search lost focus.
     * <p/>
     * <p>When called, a client would ideally grab the
     * search or suggestion query from the callback parameter or
     * from {@link #getQuery() getquery} and perform the necessary
     * query against its data source.</p>
     *
     * @param listener listener for query completion
     */
    public void setOnSearchListener(OnSearchListener listener) {
        this.mSearchListener = listener;
    }

    /**
     * Sets the listener that will be called when the focus
     * of the search has changed.
     *
     * @param listener listener for search focus changes
     */
    public void setOnFocusChangeListener(OnFocusChangeListener listener) {
        this.mFocusChangeListener = listener;
    }

    /**
     * Sets the listener that will be called when the
     * left/start menu (or navigation menu) is clicked.
     * <p/>
     * <p>Note that this is different from the overflow menu
     * that has a separate listener.</p>
     *
     * @param listener
     */
    public void setOnLeftMenuClickListener(OnLeftMenuClickListener listener) {
        this.mOnMenuClickListener = listener;
    }

    /**
     * Sets the listener that will be called when the
     * left/start home action (back arrow) is clicked.
     *
     * @param listener
     */
    public void setOnHomeActionClickListener(OnHomeActionClickListener listener) {
        this.mOnHomeActionClickListener = listener;
    }

    /**
     * Sets the listener that will be called when
     * an item in the overflow menu is clicked.
     *
     * @param listener listener to listen to menu item clicks
     */
    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.mActionMenuItemListener = listener;
        //todo reset menu view listener
    }

    private void openMenuDrawable(final DrawerArrowDrawable drawerArrowDrawable, boolean withAnim) {

        if (withAnim) {
            ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    float value = (Float) animation.getAnimatedValue();
                    drawerArrowDrawable.setProgress(value);
                }
            });
            anim.setDuration(MENU_ICON_ANIM_DURATION);
            anim.start();
        } else {
            drawerArrowDrawable.setProgress(1.0f);
        }
    }

    private void closeMenuDrawable(final DrawerArrowDrawable drawerArrowDrawable, boolean withAnim) {

        if (withAnim) {
            ValueAnimator anim = ValueAnimator.ofFloat(1.0f, 0.0f);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    float value = (Float) animation.getAnimatedValue();
                    drawerArrowDrawable.setProgress(value);
                }
            });
            anim.setDuration(MENU_ICON_ANIM_DURATION);
            anim.start();
        } else {
            drawerArrowDrawable.setProgress(0.0f);
        }
    }

    private void fadeOutBackground() {

        ValueAnimator anim = ValueAnimator.ofInt(BACKGROUND_DRAWABLE_ALPHA_SEARCH_ACTIVE, BACKGROUND_DRAWABLE_ALPHA_SEARCH_INACTIVE);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                int value = (Integer) animation.getAnimatedValue();
                mBackgroundDrawable.setAlpha(value);
            }
        });
        anim.setDuration(BACKGROUND_FADE__ANIM_DURATION);
        anim.start();
    }

    private void fadeInBackground() {

        ValueAnimator anim = ValueAnimator.ofInt(BACKGROUND_DRAWABLE_ALPHA_SEARCH_INACTIVE, BACKGROUND_DRAWABLE_ALPHA_SEARCH_ACTIVE);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                int value = (Integer) animation.getAnimatedValue();
                mBackgroundDrawable.setAlpha(value);
            }
        });
        anim.setDuration(BACKGROUND_FADE__ANIM_DURATION);
        anim.start();
    }

    private boolean isRTL() {

        Configuration config = getResources().getConfiguration();
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.suggestions = this.mSuggestionsAdapter.getDataSet();
        savedState.isFocused = this.mIsFocused;
        savedState.query = getQuery();
        savedState.suggestionTextSize = this.mSuggestionsTextSizePx;
        savedState.searchHint = this.mSearchHint;
        savedState.dismissOnOutsideClick = this.mDismissOnOutsideTouch;
        savedState.showMoveSuggestionUpBtn = this.mShowMoveUpSuggestion;
        savedState.showSearchKey = this.mShowSearchKey;
        savedState.searchCanFocus = this.mSearchCanFocus;
        savedState.isTitleSet = this.mIsTitleSet;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        this.mIsFocused = savedState.isFocused;
        this.mIsTitleSet = savedState.isTitleSet;

        setSuggestionItemTextSize(savedState.suggestionTextSize);
        setDismissOnOutsideClick(savedState.dismissOnOutsideClick);
        setShowMoveUpSuggestion(savedState.showMoveSuggestionUpBtn);
        setShowSearchKey(savedState.showSearchKey);
        setSearchHint(savedState.searchHint);

        if (this.mIsFocused) {

            mBackgroundDrawable.setAlpha(BACKGROUND_DRAWABLE_ALPHA_SEARCH_ACTIVE);
            mSkipTextChangeEvent = true;
            mSkipQueryFocusChangeEvent = true;

            mSuggestionsSection.setVisibility(VISIBLE);

            //restore suggestions list when suggestion section's height is fully set
            mSuggestionSecHeightListener = new OnSuggestionSecHeightSetListener() {
                @Override
                public void onSuggestionSecHeightSet() {
                    swapSuggestions(savedState.suggestions, false);
                    mSuggestionSecHeightListener = null;

                    //todo refactor move to a better location
                    transitionInLeftSection(false);
                }
            };

            mClearButton.setVisibility((savedState.query.length() == 0) ? View.INVISIBLE : View.VISIBLE);
            mLeftAction.setVisibility(View.VISIBLE);

            Util.showSoftKeyboard(getContext(), mSearchInput);
        }
    }

    static class SavedState extends BaseSavedState {

        private List<? extends SearchSuggestion> suggestions = new ArrayList<>();
        private boolean isFocused;
        private String query;
        private int suggestionTextSize;
        private String searchHint;
        private boolean dismissOnOutsideClick;
        private boolean showMoveSuggestionUpBtn;
        private boolean showSearchKey;
        private boolean searchCanFocus;
        private boolean isTitleSet;

        //private int leftMode;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            in.readList(suggestions, getClass().getClassLoader());
            isFocused = (in.readInt() != 0);
            query = in.readString();
            suggestionTextSize = in.readInt();
            searchHint = in.readString();
            dismissOnOutsideClick = (in.readInt() != 0);
            showMoveSuggestionUpBtn = (in.readInt() != 0);
            showSearchKey = (in.readInt() != 0);
            searchCanFocus = (in.readInt() != 0);
            isTitleSet = (in.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeList(suggestions);
            out.writeInt(isFocused ? 1 : 0);
            out.writeString(query);
            out.writeInt(suggestionTextSize);
            out.writeString(searchHint);
            out.writeInt(dismissOnOutsideClick ? 1 : 0);
            out.writeInt(showMoveSuggestionUpBtn ? 1 : 0);
            out.writeInt(showSearchKey ? 1 : 0);
            out.writeInt(searchCanFocus ? 1 : 0);
            out.writeInt(isTitleSet ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        //remove any ongoing animations to prevent leaks
        //todo investigate if correct
        ViewCompat.animate(mSuggestionListContainer).cancel();
    }
}
