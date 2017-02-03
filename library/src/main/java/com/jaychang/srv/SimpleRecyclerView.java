package com.jaychang.srv;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.SnapHelper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import com.jaychang.srv.behavior.DragAndDropCallback;
import com.jaychang.srv.behavior.DragAndDropHelper;
import com.jaychang.srv.behavior.DragAndDropOptions;
import com.jaychang.srv.behavior.SnapAlignment;
import com.jaychang.srv.behavior.StartSnapHelper;
import com.jaychang.srv.behavior.SwipeDirection;
import com.jaychang.srv.behavior.SwipeToDismissCallback;
import com.jaychang.srv.behavior.SwipeToDismissHelper;
import com.jaychang.srv.behavior.SwipeToDismissOptions;
import com.jaychang.srv.decoration.DividerItemDecoration;
import com.jaychang.srv.decoration.GridSpacingItemDecoration;
import com.jaychang.srv.decoration.LinearSpacingItemDecoration;
import com.jaychang.srv.decoration.SectionHeaderItemDecoration;
import com.jaychang.srv.decoration.SectionHeaderProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimpleRecyclerView extends RecyclerView
  implements CellOperations {

  private int layoutMode;
  private int gridSpanCount;
  private String gridSpanSequence;
  private int spacing;
  private int verticalSpacing;
  private int horizontalSpacing;
  private boolean isSpacingIncludeEdge;
  private boolean showDivider;
  private boolean showLastDivider;
  private int dividerColor;
  private int dividerOrientation;
  private int dividerPaddingLeft;
  private int dividerPaddingRight;
  private int dividerPaddingTop;
  private int dividerPaddingBottom;
  private boolean isSnappyEnabled;
  private int snapAlignment;
  private int emptyStateViewRes;

  private SimpleAdapter adapter;
  private LayoutManager layoutManager;
  private AdapterDataObserver adapterDataObserver = new AdapterDataObserver() {
    @Override
    public void onChanged() {
      updateEmptyStateViewVisibility();
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
      updateEmptyStateViewVisibility();
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
      updateEmptyStateViewVisibility();
    }
  };

  private List<String> noDividerCellTypes;

  private InternalEmptyStateViewCell emptyStateViewCell;
  private boolean isEmptyViewShown;
  private boolean isRefreshing;

  private boolean isScrollUp;
  private int autoLoadMoreThreshold;
  private OnLoadMoreListener onLoadMoreListener;
  private boolean isLoadMoreToTop;
  private boolean isLoadingMore;

  public SimpleRecyclerView(Context context) {
    this(context, null);
  }

  public SimpleRecyclerView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SimpleRecyclerView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    initAttrs(context, attrs, defStyle);

    if (!isInEditMode()) {
      setup();
    }
  }

  private void initAttrs(Context context, AttributeSet attrs, int defStyle) {
    TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.srv_SimpleRecyclerView, defStyle, 0);
    layoutMode = typedArray.getInt(R.styleable.srv_SimpleRecyclerView_srv_layoutMode, 0);
    gridSpanCount = typedArray.getInt(R.styleable.srv_SimpleRecyclerView_srv_gridSpanCount, 0);
    gridSpanSequence = typedArray.getString(R.styleable.srv_SimpleRecyclerView_srv_gridSpanSequence);
    spacing = typedArray.getDimensionPixelSize(R.styleable.srv_SimpleRecyclerView_srv_spacing, 0);
    verticalSpacing = typedArray.getDimensionPixelSize(R.styleable.srv_SimpleRecyclerView_srv_verticalSpacing, 0);
    horizontalSpacing = typedArray.getDimensionPixelSize(R.styleable.srv_SimpleRecyclerView_srv_horizontalSpacing, 0);
    isSpacingIncludeEdge = typedArray.getBoolean(R.styleable.srv_SimpleRecyclerView_srv_isSpacingIncludeEdge, false);
    showDivider = typedArray.getBoolean(R.styleable.srv_SimpleRecyclerView_srv_showDivider, false);
    showLastDivider = typedArray.getBoolean(R.styleable.srv_SimpleRecyclerView_srv_showLastDivider, false);
    dividerColor = typedArray.getColor(R.styleable.srv_SimpleRecyclerView_srv_dividerColor, 0);
    dividerOrientation = typedArray.getInt(R.styleable.srv_SimpleRecyclerView_srv_dividerOrientation, 2);
    dividerPaddingLeft = typedArray.getDimensionPixelSize(R.styleable.srv_SimpleRecyclerView_srv_dividerPaddingLeft, 0);
    dividerPaddingRight = typedArray.getDimensionPixelSize(R.styleable.srv_SimpleRecyclerView_srv_dividerPaddingRight, 0);
    dividerPaddingTop = typedArray.getDimensionPixelSize(R.styleable.srv_SimpleRecyclerView_srv_dividerPaddingTop, 0);
    dividerPaddingBottom = typedArray.getDimensionPixelSize(R.styleable.srv_SimpleRecyclerView_srv_dividerPaddingBottom, 0);
    isSnappyEnabled = typedArray.getBoolean(R.styleable.srv_SimpleRecyclerView_srv_snappy, false);
    snapAlignment = typedArray.getInt(R.styleable.srv_SimpleRecyclerView_srv_snap_alignment, 0);
    emptyStateViewRes = typedArray.getResourceId(R.styleable.srv_SimpleRecyclerView_srv_emptyStateView, 0);
    typedArray.recycle();
  }

  /**
   * setup
   */
  private void setup() {
    setupRecyclerView();
    setupDecorations();
    setupBehaviors();
  }

  private void setupRecyclerView() {
    setupAdapter();
    setupLayoutManager();
    setupEmptyView();
    setupLoadMore();
    disableChangeAnimations();
  }

  private void setupAdapter() {
    adapter = new SimpleAdapter();
    adapter.registerAdapterDataObserver(adapterDataObserver);
    setAdapter(adapter);
  }

  private void setupLayoutManager() {
    if (layoutMode == 0) {
      useLinearVerticalMode();
    } else if (layoutMode == 1) {
      useLinearHorizontalMode();
    } else if (layoutMode == 2) {
      if (!TextUtils.isEmpty(gridSpanSequence)) {
        try {
          useGridModeWithSequence(Utils.toIntList(gridSpanSequence));
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("gridSpanSequence must be digits. (e.g. 2233)");
        }
      } else {
        useGridMode(gridSpanCount);
      }
    }
  }

  private void setupEmptyView() {
    if (emptyStateViewRes != 0) {
      setEmptyStateView(emptyStateViewRes);
    }
  }

  private void setupLoadMore() {
    addOnScrollListener(new OnScrollListener() {
      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (onLoadMoreListener == null) {
          return;
        }

        isScrollUp = dy < 0 || dy == 0;

        checkLoadMoreThreshold();
      }
    });

    // trigger initial checkLoadMoreThreshold() if isLoadMoreToTop is true.
    setOnTouchListener(new OnTouchListener() {
      float preY;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (onLoadMoreListener == null || !isLoadMoreToTop) {
          return false;
        }

        switch (event.getAction()) {
          case MotionEvent.ACTION_MOVE:
            isScrollUp = event.getY() > preY;
            preY = event.getY();
            if (isScrollUp) {
              checkLoadMoreThreshold();
              setOnTouchListener(null);
            }
        }

        return false;
      }
    });
  }

  private void checkLoadMoreThreshold() {
    if (isEmptyViewShown || isLoadingMore) {
      return;
    }

    if (isLoadMoreToTop && isScrollUp) {
      int topHiddenItemCount = getFirstVisibleItemPosition();

      if (topHiddenItemCount == -1) {
        return;
      }

      if (topHiddenItemCount <= autoLoadMoreThreshold) {
        handleLoadMore();
      }

      return;
    }

    if (!isLoadMoreToTop && !isScrollUp) {
      int bottomHiddenItemCount = getItemCount() - getLastVisibleItemPosition() - 1;

      if (bottomHiddenItemCount == -1) {
        return;
      }

      if (bottomHiddenItemCount <= autoLoadMoreThreshold) {
        handleLoadMore();
      }
    }
  }

  private void handleLoadMore() {
    isLoadingMore = true;
    onLoadMoreListener.onLoadMore(this);
  }

  private int getFirstVisibleItemPosition() {
    if (layoutManager instanceof GridLayoutManager) {
      return ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
    } else if (layoutManager instanceof LinearLayoutManager) {
      return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
    } else {
      return -1;
    }
  }

  private int getLastVisibleItemPosition() {
    if (layoutManager instanceof GridLayoutManager) {
      return ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
    } else if (layoutManager instanceof LinearLayoutManager) {
      return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
    } else {
      return -1;
    }
  }

  private void disableChangeAnimations() {
    ItemAnimator animator = getItemAnimator();
    if (animator instanceof SimpleItemAnimator) {
      ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
    }
  }

  private void setupDecorations() {
    if (showDivider) {
      if (dividerColor != 0) {
        showDividerInternal(dividerColor, dividerPaddingLeft, dividerPaddingTop, dividerPaddingRight, dividerPaddingBottom);
      } else {
        showDivider();
      }
    }

    if (spacing != 0) {
      setSpacingInternal(spacing, spacing, isSpacingIncludeEdge);
    } else if (verticalSpacing != 0 || horizontalSpacing != 0){
      setSpacingInternal(verticalSpacing, horizontalSpacing, isSpacingIncludeEdge);
    }
  }

  private void setupBehaviors() {
    if (isSnappyEnabled) {
      if (snapAlignment == 0) {
        enableSnappy(SnapAlignment.CENTER);
      } else if (snapAlignment == 1) {
        enableSnappy(SnapAlignment.START);
      }
    }
  }

  /**
   * layout modes
   */
  public void useLinearVerticalMode() {
    layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    setLayoutManager(layoutManager);
  }

  public void useLinearHorizontalMode() {
    layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
    setLayoutManager(layoutManager);
  }

  public void useGridMode(int spanCount) {
    setGridSpanCount(spanCount);
    layoutManager = new GridLayoutManager(getContext(), spanCount);
    setLayoutManager(layoutManager);

    GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
      @Override
      public int getSpanSize(int position) {
        try {
          return adapter.getCell(position).getSpanSize();
        } catch (Exception e) {
          return 1;
        }
      }
    };
    spanSizeLookup.setSpanIndexCacheEnabled(true);
    ((GridLayoutManager) layoutManager).setSpanSizeLookup(spanSizeLookup);
  }

  public void useGridModeWithSequence(int first, int... rest) {
    useGridModeWithSequence(Utils.toIntList(first, rest));
  }

  public void useGridModeWithSequence(List<Integer> sequence) {
    final int lcm = Utils.lcm(sequence);
    final ArrayList<Integer> sequenceList = new ArrayList<>();
    for (int i = 0; i < sequence.size(); i++) {
      int item = sequence.get(i);
      for (int j = 0; j < item; j++) {
        sequenceList.add(lcm / item);
      }
    }

    setGridSpanCount(lcm);
    layoutManager = new GridLayoutManager(getContext(), lcm);
    setLayoutManager(layoutManager);

    GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
      @Override
      public int getSpanSize(int position) {
        try {
          return sequenceList.get(position % sequenceList.size());
        } catch (Exception e) {
          return 1;
        }
      }
    };
    spanSizeLookup.setSpanIndexCacheEnabled(true);
    ((GridLayoutManager) layoutManager).setSpanSizeLookup(spanSizeLookup);
  }

  private void setGridSpanCount(int spanCount) {
    if (spanCount <= 0) {
      throw new IllegalArgumentException("spanCount must >= 1");
    }

    this.gridSpanCount = spanCount;
  }

  /**
   * divider
   */
  private void showDividerInternal(@ColorInt int color,
                                   int paddingLeft, int paddingTop,
                                   int paddingRight, int paddingBottom) {
    if (layoutManager instanceof GridLayoutManager) {
      if (dividerOrientation == 0) {
        addDividerItemDecoration(color, DividerItemDecoration.HORIZONTAL,
          paddingLeft, paddingTop, paddingRight, paddingBottom);
      } else if (dividerOrientation == 1) {
        addDividerItemDecoration(color, DividerItemDecoration.VERTICAL,
          paddingLeft, paddingTop, paddingRight, paddingBottom);
      } else {
        addDividerItemDecoration(color, DividerItemDecoration.VERTICAL,
          paddingLeft, paddingTop, paddingRight, paddingBottom);
        addDividerItemDecoration(color, DividerItemDecoration.HORIZONTAL,
          paddingLeft, paddingTop, paddingRight, paddingBottom);
      }
    } else if (layoutManager instanceof LinearLayoutManager) {
      int orientation = ((LinearLayoutManager) layoutManager).getOrientation();
      addDividerItemDecoration(color, orientation,
        paddingLeft, paddingTop, paddingRight, paddingBottom);
    }
  }

  private void addDividerItemDecoration(@ColorInt int color, int orientation,
                                        int paddingLeft, int paddingTop,
                                        int paddingRight, int paddingBottom) {
    DividerItemDecoration decor = new DividerItemDecoration(getContext(), orientation);
    if (color != 0) {
      ShapeDrawable shapeDrawable = new ShapeDrawable(new RectShape());
      shapeDrawable.setIntrinsicHeight(Utils.dpToPx(getContext(), 1));
      shapeDrawable.setIntrinsicWidth(Utils.dpToPx(getContext(), 1));
      shapeDrawable.getPaint().setColor(color);
      InsetDrawable insetDrawable = new InsetDrawable(shapeDrawable, paddingLeft, paddingTop, paddingRight, paddingBottom);
      decor.setDrawable(insetDrawable);
    }
    decor.setShowLastDivider(showLastDivider);
    addItemDecoration(decor);
  }

  public void showDivider() {
    showDividerInternal(Color.parseColor("#e0e0e0"), dividerPaddingLeft, dividerPaddingTop, dividerPaddingRight, dividerPaddingBottom);
  }

  public void showDivider(@ColorRes int colorRes) {
    showDividerInternal(ContextCompat.getColor(getContext(), colorRes),
      dividerPaddingLeft, dividerPaddingTop, dividerPaddingRight, dividerPaddingBottom);
  }

  public void showDivider(@ColorRes int colorRes, int paddingLeftDp, int paddingTopDp, int paddingRightDp, int paddingBottomDp) {
    showDividerInternal(ContextCompat.getColor(getContext(), colorRes),
      Utils.dpToPx(getContext(), paddingLeftDp), Utils.dpToPx(getContext(), paddingTopDp),
      Utils.dpToPx(getContext(), paddingRightDp), Utils.dpToPx(getContext(), paddingBottomDp));
  }

  public void dontShowDividerForCellType(Class<?>... classes) {
    if (noDividerCellTypes == null) {
      noDividerCellTypes = new ArrayList<>();
    }

    for (Class<?> aClass : classes) {
      noDividerCellTypes.add(aClass.getSimpleName());
    }
  }

  public List<String> getNoDividerCellTypes() {
    return noDividerCellTypes == null ? Collections.<String>emptyList() : noDividerCellTypes;
  }

  /**
   * spacing
   */
  private void setGridSpacingInternal(int verSpacing, int horSpacing, boolean includeEdge) {
    addItemDecoration(GridSpacingItemDecoration.newBuilder().verticalSpacing(verSpacing).horizontalSpacing(horSpacing).includeEdge(includeEdge).build());
  }

  private void setLinearSpacingInternal(int spacing, boolean includeEdge) {
    int orientation = ((LinearLayoutManager) layoutManager).getOrientation();
    addItemDecoration(LinearSpacingItemDecoration.newBuilder().spacing(spacing).orientation(orientation).includeEdge(includeEdge).build());
  }

  private void setSpacingInternal(int verSpacing, int horSpacing, boolean includeEdge) {
    if (layoutManager instanceof GridLayoutManager) {
      setGridSpacingInternal(verSpacing, horSpacing, includeEdge);
    } else if (layoutManager instanceof LinearLayoutManager) {
      setLinearSpacingInternal(verSpacing, includeEdge);
    }
  }

  public void setSpacing(int spacingDp) {
    int spacing = Utils.dpToPx(getContext(), spacingDp);
    setSpacingInternal(spacing, spacing, false);
  }

  public void setSpacingIncludeEdge(int spacingDp) {
    int spacing = Utils.dpToPx(getContext(), spacingDp);
    setSpacingInternal(spacing, spacing, true);
  }

  public void setSpacing(int verticalSpacingDp, int horizontalSpacingDp) {
    int verticalSpacing = Utils.dpToPx(getContext(), verticalSpacingDp);
    int horizontalSpacing = Utils.dpToPx(getContext(), horizontalSpacingDp);
    setSpacingInternal(verticalSpacing, horizontalSpacing, false);
  }

  public void setSpacingIncludeEdge(int verticalSpacingDp, int horizontalSpacingDp) {
    int verticalSpacing = Utils.dpToPx(getContext(), verticalSpacingDp);
    int horizontalSpacing = Utils.dpToPx(getContext(), horizontalSpacingDp);
    setSpacingInternal(verticalSpacing, horizontalSpacing, true);
  }

  /**
   * empty view
   */
  private void updateEmptyStateViewVisibility() {
    adapter.unregisterAdapterDataObserver(adapterDataObserver);
    if (adapter.getItemCount() <= 0) {
      showEmptyStateView();
    } else {
      hideEmptyStateView();
    }
    adapter.registerAdapterDataObserver(adapterDataObserver);
  }

  private void showEmptyStateView() {
    if (isRefreshing) {
      isRefreshing = false;
      return;
    }

    if (isEmptyViewShown || emptyStateViewCell == null) {
      return;
    }

    addCell(emptyStateViewCell);

    isEmptyViewShown = true;
  }

  private void hideEmptyStateView() {
    if (!isEmptyViewShown || emptyStateViewCell == null) {
      return;
    }

    removeCell(emptyStateViewCell);

    isEmptyViewShown = false;
  }

  public void setEmptyStateView(@LayoutRes int emptyStateView) {
    View view = LayoutInflater.from(getContext()).inflate(emptyStateView, this, false);
    setEmptyStateView(view);
  }

  public void setEmptyStateView(View emptyStateView) {
    this.emptyStateViewCell = new InternalEmptyStateViewCell(emptyStateView);
    emptyStateViewCell.setSpanSize(gridSpanCount);
  }

  /**
   * load more
   */
  public void setAutoLoadMoreThreshold(int hiddenCellCount) {
    if (hiddenCellCount < 0) {
      throw new IllegalArgumentException("hiddenCellCount must >= 0");
    }
    this.autoLoadMoreThreshold = hiddenCellCount;
  }

  public int getAutoLoadMoreThreshold() {
    return autoLoadMoreThreshold;
  }

  public void setLoadMoreToTop(boolean isLoadMoreForTop) {
    this.isLoadMoreToTop = isLoadMoreForTop;
  }

  public boolean isLoadMoreToTop() {
    return isLoadMoreToTop;
  }

  public void setOnLoadMoreListener(OnLoadMoreListener listener) {
    this.onLoadMoreListener = listener;
  }

  // Need to call this method to tell SimpleRecyclerView to resume checking threshold,
  public void setLoadMoreCompleted() {
    this.isLoadingMore = false;
  }

  /**
   * drag & drop
   */
  public void enableDragAndDrop(DragAndDropCallback dragAndDropCallback) {
    enableDragAndDrop(0, dragAndDropCallback);
  }

  public void enableDragAndDrop(@IdRes int dragHandleId, DragAndDropCallback dragAndDropCallback) {
    DragAndDropOptions options = new DragAndDropOptions();
    options.setDragHandleId(dragHandleId);
    options.setCanLongPressToDrag(dragHandleId == 0);
    options.setDragAndDropCallback(dragAndDropCallback);
    options.setEnableDefaultEffect(dragAndDropCallback.enableDefaultRaiseEffect());
    DragAndDropHelper dragAndDropHelper = DragAndDropHelper.create(adapter, options);
    adapter.setDragAndDropHelper(dragAndDropHelper);
    dragAndDropHelper.attachToRecyclerView(this);
  }

  /**
   * swipe to dismiss
   */
  public void enableSwipeToDismiss(SwipeToDismissCallback swipeToDismissCallback, SwipeDirection... directions) {
    enableSwipeToDismiss(swipeToDismissCallback, new HashSet<>(Arrays.asList(directions)));
  }

  public void enableSwipeToDismiss(SwipeToDismissCallback swipeToDismissCallback, Set<SwipeDirection> directions) {
    SwipeToDismissOptions options = new SwipeToDismissOptions();
    options.setEnableDefaultFadeOutEffect(swipeToDismissCallback.enableDefaultFadeOutEffect());
    options.setSwipeToDismissCallback(swipeToDismissCallback);
    options.setSwipeDirections(directions);
    SwipeToDismissHelper helper = SwipeToDismissHelper.create(adapter, options);
    helper.attachToRecyclerView(this);
  }

  /**
   * snappy
   */
  public void enableSnappy() {
    enableSnappy(SnapAlignment.CENTER);
  }

  public void enableSnappy(SnapAlignment alignment) {
    SnapHelper snapHelper = alignment.equals(SnapAlignment.CENTER) ?
      new LinearSnapHelper() : new StartSnapHelper(spacing);
    snapHelper.attachToRecyclerView(this);
  }

  /**
   * section header
   */
  public void setSectionHeader(SectionHeaderProvider<?> provider) {
    if (layoutManager instanceof GridLayoutManager) {
      // todo
      return;
    }
    if (layoutManager instanceof LinearLayoutManager) {
      addItemDecoration(new SectionHeaderItemDecoration(provider));
    }
  }

  /**
   * cell operations
   */
  @Override
  public void addCell(SimpleCell cell) {
    adapter.addCell(cell);
  }

  @Override
  public void addCell(int atPosition, SimpleCell cell) {
    adapter.addCell(atPosition, cell);
  }

  @Override
  public void addCells(List<? extends SimpleCell> cells) {
    adapter.addCells(cells);
  }

  @Override
  public void addCells(SimpleCell... cells) {
    adapter.addCells(cells);
  }

  @Override
  public void addCells(int fromPosition, List<? extends SimpleCell> cells) {
    adapter.addCells(fromPosition, cells);
  }

  @Override
  public void addCells(int fromPosition, SimpleCell... cells) {
    adapter.addCells(fromPosition, cells);
  }

  @Override
  public <T extends SimpleCell & Updatable> void addOrUpdateCell(T cell) {
    adapter.addOrUpdateCell(cell);
  }

  @Override
  public <T extends SimpleCell & Updatable> void addOrUpdateCells(List<T> cells) {
    adapter.addOrUpdateCells(cells);
  }

  @Override
  public <T extends SimpleCell & Updatable> void addOrUpdateCells(T... cells) {
    adapter.addOrUpdateCells(cells);
  }

  @Override
  public void removeCell(SimpleCell cell) {
    adapter.removeCell(cell);
  }

  @Override
  public void removeCell(int atPosition) {
    adapter.removeCell(atPosition);
  }

  @Override
  public void removeCells(int fromPosition, int toPosition) {
    adapter.removeCells(fromPosition, toPosition);
  }

  @Override
  public void removeCells(int fromPosition) {
    adapter.removeCells(fromPosition);
  }

  @Override
  public void updateCell(int atPosition, Object payload) {
    adapter.updateCell(atPosition, payload);
  }

  @Override
  public void updateCells(int fromPosition, int toPosition, List<Object> payloads) {
    adapter.updateCells(fromPosition, toPosition, payloads);
  }

  @Override
  public SimpleCell getCell(int atPosition) {
    return adapter.getCell(atPosition);
  }

  @Override
  public List<SimpleCell> getCells(int fromPosition, int toPosition) {
    return adapter.getCells(fromPosition, toPosition);
  }

  @Override
  public List<SimpleCell> getAllCells() {
    return adapter.getAllCells();
  }

  @Override
  public void removeAllCells() {
    removeAllCells(true);
  }

  // remove all cells and indicates that data is refreshing, so the empty view will not be shown.
  public void removeAllCells(boolean showEmptyStateView) {
    this.isRefreshing = !showEmptyStateView;
    adapter.removeAllCells();
  }

  public boolean isEmpty() {
    return getItemCount() <= 0;
  }

  public int getItemCount() {
    return isEmptyViewShown ? 0 : adapter.getItemCount();
  }

  /**
   * common
   */
  public int getGridSpanCount() {
    return gridSpanCount;
  }

}