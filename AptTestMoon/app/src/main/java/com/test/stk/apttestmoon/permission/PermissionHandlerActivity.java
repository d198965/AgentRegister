package com.test.stk.apttestmoon.permission;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.provider.Settings;

public class PermissionHandlerActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 0;
    private static final int OPEN_APPLICATION_SETTING_CODE = 2;

    private PermissionRequestInfo requestInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNextPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PermissionCheckHelper.instance().setRequestFinish();
    }

    /**
     * 处理下一个权限请求
     */
    private void requestNextPermission(){
        requestInfo = PermissionCheckHelper.instance().getNextRequest();
        if(requestInfo != null){
            String[] permissionArray = requestInfo.getPermissionArray();
            if(permissionArray != null && permissionArray.length == 1){
                handleSinglePermissionCheck(permissionArray[0], requestInfo.getMessageArray()[0]);
            } else if (permissionArray != null && permissionArray.length > 1) {
                handleMultiplePermissionCheck(permissionArray, requestInfo.getMessageArray());
            }
        } else {
            PermissionHandlerActivity.this.finish();
        }
    }

    /**
     * 单个权限处理
     *
     * @param permission
     * @param message
     */
    private void handleSinglePermissionCheck(final String permission, String message){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            showNormalRationaleDialog(this, message,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(PermissionHandlerActivity.this,
                                    new String[]{permission}, PERMISSION_REQUEST_CODE);
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * 多个权限处理
     *
     * @param permissionArray
     * @param messageArray
     */
    private void handleMultiplePermissionCheck(final String[] permissionArray, String[] messageArray){
        if(permissionArray.length == 1){
            handleSinglePermissionCheck(permissionArray[0], messageArray[0]);
            return;
        }
        boolean shouldRationale = false;
        String rationaleMsg = "";
        for (int index = 0; index < permissionArray.length; ++index){
            String permission = permissionArray[index];
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                shouldRationale = true;
                if(!messageArray[index].isEmpty()) {
                    rationaleMsg += messageArray[index];
                }
            }
        }
        if(shouldRationale){
            showNormalRationaleDialog(this, rationaleMsg,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(PermissionHandlerActivity.this,
                                    permissionArray, PERMISSION_REQUEST_CODE);
                        }
                    });
            return;
        }
        ActivityCompat.requestPermissions(PermissionHandlerActivity.this, permissionArray, PERMISSION_REQUEST_CODE);
    }


    /**
     * 被拒绝的权限并且shouldShowRequestPermissionRationale返回false就是用户选中Never Ask Again的权限
     * 弹框提示用户去设置里授予权限，不请求权限
     *
     * @param permissionArray
     * @param messageArray
     * @return  true表示处理了Never Ask Again
     */
    private boolean handleNeverAsk(final String[] permissionArray, String[] messageArray, int[] grantResults){
        boolean hasNeverAsk = false;
        String rationaleMsg = "";
        for (int index = 0; index < permissionArray.length && grantResults[index] == PackageManager.PERMISSION_DENIED; ++index){
            String permission = permissionArray[index];
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                hasNeverAsk = true;
                if(!messageArray[index].isEmpty()) {
                    rationaleMsg += messageArray[index];
                }
            }
        }
        if(hasNeverAsk){
            showNeverAskRationaleDialog(this, rationaleMsg);
        }
        return hasNeverAsk;
    }

    /**
     * 权限请求结果回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(PERMISSION_REQUEST_CODE == requestCode) {
            for(int index = 0; index < permissions.length; ++index){
                requestInfo.getPermissionResultMap().put(permissions[index], grantResults[index]);
            }
            if(!handleNeverAsk(permissions, requestInfo.getMessageArray(), grantResults)) {
                doCallback();
                requestNextPermission();
            }
        }
    }

    /**
     *  回调最终权限请求结果
     */
    private void doCallback(){
        PermissionCheckHelper.PermissionCallbackListener listener = requestInfo.getListener();
        if (listener != null) {
            listener.onPermissionCheckCallback(requestInfo.getRequestCode(),
                    requestInfo.getRequestPermissions(), requestInfo.getRequestResults());
        }
    }

    /**
     * 打开设置中app应用信息界面
     *
     */
    private void openAppSetting(){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, OPEN_APPLICATION_SETTING_CODE);
    }

    /**
     * 处理Never Ask Again情况，用户返回后再次去请求权限来获取最终的权限请求结果
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == OPEN_APPLICATION_SETTING_CODE) {
            requestInfo = PermissionCheckHelper.instance().findShouldCheckPermission(requestInfo);
            if(requestInfo != null) {
                doCallback();
            }
            requestNextPermission();
        }
    }

    /**
     * 自定义权限请求解释提示框
     *
     * @param context
     * @param message
     * @param okListener
     */
    private void showNormalRationaleDialog(Context context, String message,
                                           DialogInterface.OnClickListener okListener) {
        if(!"".equals(message)){
            message = message.substring(0, message.length() - 1);
        }
        StringBuilder builder = new StringBuilder(message)
                .append("。");
        new AlertDialog.Builder(context)
                .setTitle("我们需要一些权限")
                .setMessage(builder)
                .setPositiveButton("确定", okListener)
                .setCancelable(false).create().show();
    }

    /**
     * 处理Never Ask Again
     * 自定义权限请求解释提示框，带有设置引导
     *
     * @param context
     * @param message
     */
    private void showNeverAskRationaleDialog(Context context, String message) {
        if(!"".equals(message)){
            message = message.substring(0, message.length() - 1);
        }
        StringBuilder builder = new StringBuilder(message)
                .append("。")
                .append("\n设置路径：设置->应用->权限");
        new AlertDialog.Builder(context)
                .setTitle("我们需要一些权限")
                .setMessage(builder)
                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openAppSetting();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doCallback();
                        requestNextPermission();
                    }
                }).setCancelable(false).create().show();
    }

}