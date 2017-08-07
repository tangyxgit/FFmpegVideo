package com.tangyx.video;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.tangyx.video.helper.PermissionHelper;

/**
 * 权限获取页面
 * <p/>
 */
public class PermissionsActivity extends AppCompatActivity {
    //基本权限必须有
    public final static String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };
    public static final int PERMISSIONS_GRANTED = 1010; // 权限授权
    public static final int PERMISSIONS_DENIED = 1011; // 权限拒绝

    public static final int REQUEST_CODE = 1012; // 请求码

    private static final int PERMISSION_REQUEST_CODE = 0; // 系统权限管理页面的参数
    private static final String EXTRA_PERMISSIONS =
            "megawave.permission.extra_permission"; // 权限参数
    private static final String PACKAGE_URL_SCHEME = "package:"; // 方案

    private PermissionHelper mChecker; // 权限检测器
    private boolean isRequireCheck; // 是否需要系统权限检测, 防止和系统提示框重叠
    private static boolean isShowSetting=true;

    // 启动当前权限页面的公开接口
    public static void startActivityForResult(Activity activity, int requestCode, String... permissions) {
        startActivityForResult(activity,requestCode,true,permissions);
    }

    public static void startActivityForResult(Activity activity, int requestCode,boolean showSetting,String... permissions) {
        Intent intent = new Intent(activity, PermissionsActivity.class);
        intent.putExtra(EXTRA_PERMISSIONS, permissions);
        ActivityCompat.startActivityForResult(activity, intent, requestCode, null);
        isShowSetting = showSetting;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || !getIntent().hasExtra(EXTRA_PERMISSIONS)) {
            throw new RuntimeException("PermissionsActivity需要使用静态startActivityForResult方法启动!");
        }
        setContentView(R.layout.activity_permissions);
        mChecker = new PermissionHelper(this);
        isRequireCheck = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRequireCheck) {
            String[] permissions = getPermissions();
            if (mChecker.lacksPermissions(permissions)) {
                requestPermissions(permissions); // 请求权限
            } else {
                allPermissionsGranted(); // 全部权限都已获取
            }
        } else {
            isRequireCheck = true;
        }
    }

    // 返回传递的权限参数
    private String[] getPermissions() {
        return getIntent().getStringArrayExtra(EXTRA_PERMISSIONS);
    }

    // 请求权限兼容低版本
    private void requestPermissions(String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    // 全部权限均已获取
    private void allPermissionsGranted() {
        setResult(PERMISSIONS_GRANTED);
        finish();
    }

    /**
     * 用户权限处理,
     * 如果全部获取, 则直接过.
     * 如果权限缺失, 则提示Dialog.
     *
     * @param requestCode  请求码
     * @param permissions  权限
     * @param grantResults 结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && hasAllPermissionsGranted(grantResults)) {
            isRequireCheck = true;
            allPermissionsGranted();
        } else {
            isRequireCheck = false;
            if(isShowSetting){
                showMissingPermissionDialog();
            }
        }
    }

    // 含有全部的权限
    private boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    // 显示缺失权限提示
    public void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PermissionsActivity.this);
        builder.setTitle(R.string.label_help);
        builder.setMessage(R.string.tips_permissions);

        // 拒绝, 退出应用
        builder.setNegativeButton(R.string.label_quit, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                setResult(-100);
                finish();
            }
        });

        builder.setPositiveButton(R.string.label_setting, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                startAppSettings();
            }
        });

        builder.setCancelable(false);

        builder.show();
    }

    // 启动应用的设置
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(PACKAGE_URL_SCHEME + getPackageName()));
        startActivity(intent);
    }
}
