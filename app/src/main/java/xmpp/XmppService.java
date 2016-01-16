package xmpp;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

public class XmppService extends Service {

    public static final String NEW_MESSAGE = "com.example.feliche.newmessage";
    public static final String SEND_MESSAGE = "com.example.feliche.sendmessage";
    public static final String UPDATE_CONNECTION = "com.example.feliche.statusconnection";
    public static final String SMS_CONNECTION = "android.provider.Telephony.SMS_RECEIVED";
    public static final String CHANGE_CONNECTIVITY = "android.net.conn.CONNECTIVITY_CHANGE";

    public static final String NEW_VCARD = "com.example.feliche.newvcard";
    public static final String GET_VCARD = "com.example.feliche.getvcard";

    // id de nuevo mensaje del multiuser chat
    public static final String NEW_MUC_MESSAGE = "com.example.feliche.newmucmessage";


    public static final String BUNDLE_FROM_JID = "b_from";
    public static final String BUNDLE_FROM_XMPP = "b_from";
    public static final String BUNDLE_MESSAGE_BODY = "b_body";
    public static final String BUNDLE_TO = "b_to";
    public static final String CONNECTION = "connection";

    public static final String VCARD = "new_vcard";
    public static final String ACCOUNT = "get_vcard";

    // id del remitente del mensaje multiuser chat
    // y el contenido del mensaje
    public static final String BUNDLE_MUC_JID = "bundle_muc_jid";
    public static final String BUNDLE_MUC_BODY = "bundle_muc_body";


    private boolean mActive;
    private Thread mThread;
    private Handler mTHandler;
    private XmppConnection mConnection;

    public static XmppConnection.ConnectionState sConnectionState;

    public static XmppConnection.ConnectionState getState(){
        if(sConnectionState == null){
            return XmppConnection.ConnectionState.DISCONNECTED;
        }
        return sConnectionState;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        start();
        return Service.START_STICKY;
    }

    private void start() {
        if(!mActive ){
            mActive = true;
            if(mThread == null || !mThread.isAlive()){
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        mTHandler = new Handler();
                        initConnection();
                        Looper.loop();
                    }
                });
                mThread.start();
            }
        }
    }


    public void stop(){
        mActive = false;
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mConnection != null){
                    mConnection.disconnect();
                }
            }
        });
    }

    private void initConnection(){
        String error;
        if(mConnection == null){
            mConnection = new XmppConnection(this);
        }
        try {
            mConnection.connect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (SmackException e) {
            e.printStackTrace();
        }
    }

    public XmppService(){
    }
    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}