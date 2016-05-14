package com.example.siddharth.servcomp3;

import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

/**
 * Created by siddharth on 5/11/16.
 */
public class ServiceGraphRefresh extends AsyncTask<Void, Void, Void> {

    MainActivity objMainAct;

    //Constructor
    public ServiceGraphRefresh (MainActivity _ojbMainAct){

        this.objMainAct = _ojbMainAct;
    }

    @Override
    protected void onPreExecute() {
        objMainAct.displayToast("Refreshing compositions...Please wait", Toast.LENGTH_SHORT);
    }

    @Override
    protected Void doInBackground(Void... params) {

        objMainAct.objTaskHandler.objServiceGraph.refreshServiceGraph();
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {

        objMainAct.composedServiceAdapter.notifyDataSetChanged();

        if(objMainAct.composedServiceAdapter.getCount() >0)
            objMainAct.composedServicesLstView.setVisibility(View.VISIBLE);
        else
            objMainAct.displayToast("No compositions found.", Toast.LENGTH_SHORT);
    }
}
