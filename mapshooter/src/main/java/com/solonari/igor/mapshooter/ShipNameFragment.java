package com.solonari.igor.mapshooter;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


public class ShipNameFragment extends DialogFragment {

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        String ship = getArguments().getString("shipName");

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View myLayout = inflater.inflate(R.layout.ship_name, null);
        builder.setView(myLayout)
                // Add action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String shipName = ((EditText) getDialog().findViewById(R.id.newShipName)).getText().toString();
                        // Send the positive button event back to the host activity
                        mListener.onDialogPositiveClick(ShipNameFragment.this, shipName);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String shipName = ((EditText) getDialog().findViewById(R.id.newShipName)).getText().toString();
                        mListener.onDialogNegativeClick(ShipNameFragment.this, shipName);
                        ShipNameFragment.this.getDialog().cancel();
                    }
                });
        //builder.create();
        Dialog dialog = builder.create();

        EditText shipName = (EditText) myLayout.findViewById(R.id.newShipName);
        shipName.setText(ship);

        return dialog;
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, String shipName);
        public void onDialogNegativeClick(DialogFragment dialog, String shipName);
    }

}
