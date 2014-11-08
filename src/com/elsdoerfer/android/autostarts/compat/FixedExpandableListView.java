package com.elsdoerfer.android.autostarts.compat;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ExpandableListView;


/**
 * To use fastScroll="true" with a ExpandableListView.
 */
public class FixedExpandableListView extends ExpandableListView {

	public FixedExpandableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FixedExpandableListView(Context context) {
		super(context);
	}

	public FixedExpandableListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Android's "android.widget.FastScroll" class has a bug when
	 * used with an ExpandableListView. When calculating the
	 * scroll target position, it mistakenly uses a flat index as
	 * a group position.
	 *
	 * Specifically, it calls this function with a packed group
	 * position that really is already the correct flat position.
	 *
	 * So we work around this by simply returning the incoming
	 * group position as the desired result. This only works of
	 * course as long as the FastScroll feature is the only thing
	 * using this method, which seems to be be the case.
	 *
	 * The result differs slightly from what FastScroll itself
	 * appears to attempt, namely instead of targeting only group
	 * items in the scroll, we target all items, which isn't as
	 * nice, but at least it works at all.
	 *
	 * It might also be possible to work around this by implementing
	 * a SectionIndex, but that is too much work right now.
	 *
	 * https://review.source.android.com/#change,22024
	 */
	@Override
	public int getFlatListPosition(long packedPosition) {
		return getPackedPositionGroup(packedPosition);
    }
}
