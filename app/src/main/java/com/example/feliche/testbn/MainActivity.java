package com.example.feliche.testbn;


import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import xmpp.XmppConnection;
import xmpp.XmppService;

public class MainActivity extends AppCompatActivity {
    Button btnConnect;
    Button btnSend;
    EditText etAccount;
    EditText etPassword;
    EditText etTo;

    TextView tvMessage;
    ImageView imageView1;

    private static MainActivity inst;
    private static boolean statusBroadcastReceiver;
    private WakefulBroadcastReceiver mReceiver;
    ConnectivityManager conn;

    private static final String LOGTAG = "MainActivity:BR";
    String accountXmpp, passwordXmpp;

    private static NetworkInfo networkInfo;
    private static boolean userConnection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        btnConnect = (Button)findViewById(R.id.btnConnect);
        btnSend = (Button)findViewById(R.id.btnSend);
        etAccount = (EditText)findViewById(R.id.etAccount);
        etPassword = (EditText)findViewById(R.id.etPassword);
        etTo = (EditText)findViewById(R.id.etTo);

        imageView1 = (ImageView)findViewById(R.id.imageView);

        tvMessage = (TextView)findViewById(R.id.tvMsg);

        // lee el usuario y password de memoria no volátil
        // si no existe coloca los default de New user
        accountXmpp = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Def.XMPP_ACCOUNT, Def.NEW_USER_ACCOUNT);
        passwordXmpp = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Def.XMPP_PASSWORD, Def.NEW_USER_PASS);

        if(XmppService.getState().equals(XmppConnection.ConnectionState.DISCONNECTED)){
            btnConnect.setText("Desconectado");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume(){
        super.onResume();
        mReceiver = new WakefulBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(LOGTAG, "action=" + action);
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                switch (action){
                    // mensaje XMPP
                    case XmppService.NEW_MESSAGE:
                        String from = intent.getStringExtra(XmppService.BUNDLE_FROM_JID);
                        String message = intent.getStringExtra(XmppService.BUNDLE_MESSAGE_BODY);
                        tvMessage.append(from.split("@")[0]);
                        tvMessage.append(message);
                        v.vibrate(500);
                        break;
                    // Estado de la conexión XMPP
                    case XmppService.UPDATE_CONNECTION:
                        String status = intent.getStringExtra(XmppService.CONNECTION);
                        Log.d(LOGTAG,"UpdateConnection" + status);
                        tvMessage.append(" status: " + status);
                        if (status.contains("AUTHENTICATE")) {
                            if (accountXmpp.contains(Def.NEW_USER_ACCOUNT))
                                sendMessage();
                            else
                                tvMessage.append("\n" + "CONECTADO CON USUARIO REGISTRADO");
                            btnConnect.setText("Conectado");
                        }else if(status.contains("DISCONNECTED")) {
                            btnConnect.setText("Desconectado");
                        }else if(status.contains("CLOSED_ERROR")){
                            btnConnect.setText("Desconectado");
                        }

                        break;
                    // Cambio de la conexión a Internet
                    case XmppService.CHANGE_CONNECTIVITY:
                        if(userConnection == true) {
                            if(haveInternet() == true) {
                                connectXmpp();
                                tvMessage.append("connectXmpp :");
                            }else {
                                disconnectXmpp();
                                tvMessage.append("disconnectXmpp :");
                            }
                        }
                        break;
                    case XmppService.NEW_VCARD:
                        Bundle bundleVcard = intent.getBundleExtra(XmppService.VCARD);
                        if (bundleVcard != null) {
                            byte[] avatar = bundleVcard.getByteArray("avatar");
                            String nameAvatar = bundleVcard.getString("emailHome");
                            Bitmap bmp = BitmapFactory.decodeByteArray(
                                    avatar, 0, avatar.length);
                            imageView1.setImageBitmap(bmp);
                        }
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter(XmppService.UPDATE_CONNECTION);
        filter.addAction(XmppService.NEW_MESSAGE);
        filter.addAction(XmppService.CHANGE_CONNECTIVITY);

        filter.addAction(XmppService.NEW_VCARD);
        filter.addAction(XmppService.NEW_MUC_MESSAGE);      // agregar el filtro del multiuserchat
        registerReceiver(mReceiver, filter);
        statusBroadcastReceiver = true;
    }

    private void sendMessage() {
        // se requiere de un tiempo para establecer la
        // conexión antes de enviar el mensaje
        try {
            Thread.sleep(Def.TIME_CONNECT_TO_XMPP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // envia el dato al administrador
        String adminNewUser = Def.ADMIN_NEW_USER + "@" + Def.SERVER_NAME;
        // forma la cadena a enviar en formato json de google

        Intent intent = new Intent(XmppService.SEND_MESSAGE);
        intent.setPackage(this.getPackageName());
        intent.putExtra(XmppService.BUNDLE_MESSAGE_BODY, "Mensaje");
        intent.putExtra(XmppService.BUNDLE_TO, adminNewUser);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        this.sendBroadcast(intent);

        // solicitar VCard

        Intent intentVCard = new Intent(XmppService.GET_VCARD);
        intentVCard.setPackage(this.getPackageName());
        intentVCard.putExtra(XmppService.ACCOUNT, "feliche@htu.isramoon.xyz");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        this.sendBroadcast(intentVCard);



        // espera a que el mensaje sea enviado antes de cerrar la conexión
        try {
            Thread.sleep(Def.TIME_TO_SEND_MESSAGE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // cierra la conexión
        // NO cierro la conexión para probar el multiuserchat
        //connectXmpp();
    }

    public void onClickaddConnect(View v) {
        if(userConnection == false){
            userConnection = true;
            connectXmpp();
        }else {
            userConnection = false;
            disconnectXmpp();
        }
    }

    public void connectXmpp(){
        if(XmppService.getState().equals(XmppConnection.ConnectionState.DISCONNECTED) ||
                XmppService.getState().equals(XmppConnection.ConnectionState.CLOSED_ERROR)){
            if(haveInternet() == true) {
                Intent intent = new Intent(this, XmppService.class);
                this.startService(intent);
            }
        }
    }

    public void disconnectXmpp(){
        if(XmppService.getState().equals(XmppConnection.ConnectionState.AUTHENTICATE) ||
                XmppService.getState().equals(XmppConnection.ConnectionState.CONNECTED)) {
            Intent intent = new Intent(this, XmppService.class);
            this.stopService(intent);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //if (statusBroadcastReceiver)
            statusBroadcastReceiver = true;
         //   this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onPause(){
        super.onPause();
        //if(statusBroadcastReceiver == true)
        //    this.unregisterReceiver(mReceiver);
    }

    private void saveUserPass(String accountXmpp, String passwordXmpp) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putString(Def.XMPP_ACCOUNT, accountXmpp)
                .putString(Def.XMPP_PASSWORD, String.valueOf(passwordXmpp))
                .apply();
    }

    private boolean haveInternet(){
        boolean estado;
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork == null)
            return false;
        else
            return true;
    }
}
