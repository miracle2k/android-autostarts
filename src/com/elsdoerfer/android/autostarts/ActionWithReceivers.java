package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;

import com.elsdoerfer.android.autostarts.DatabaseHelper.ReceiverData;

/**
 * A particular action and a list of receivers that register for
 * the action.
 */
class ActionWithReceivers {
	public String action;
	public ArrayList<ReceiverData> receivers;

	ActionWithReceivers(String action, ArrayList<ReceiverData> receivers) {
		this.action = action;
		this.receivers = receivers;
	}

	ActionWithReceivers(ActionWithReceivers clone) {
		this.action = clone.action;
		this.receivers = clone.receivers;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ActionWithReceivers)
			return action.equals(((ActionWithReceivers)o).action);
		else
			return action.equals(o);
	}

	@Override
	public int hashCode() { return action.hashCode(); }
}