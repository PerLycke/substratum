/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;

import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import projekt.substratum.activities.launch.ShowcaseActivity;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Restore;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.common.analytics.FirebaseAnalytics;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.AndromedaService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.databinding.MainActivityBinding;
import projekt.substratum.fragments.ManagerFragment;
import projekt.substratum.fragments.PriorityListFragment;
import projekt.substratum.fragments.PriorityLoaderFragment;
import projekt.substratum.fragments.ProfileFragment;
import projekt.substratum.fragments.SettingsFragment;
import projekt.substratum.fragments.ThemeFragment;
import projekt.substratum.services.binder.AndromedaBinderService;
import projekt.substratum.services.floatui.SubstratumFloatInterface;
import projekt.substratum.services.tiles.FloatUiTile;
import projekt.substratum.util.helpers.BinaryInstaller;
import projekt.substratum.util.helpers.LocaleHelper;
import projekt.substratum.util.helpers.Root;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS;
import static projekt.substratum.common.Activities.launchActivityUrl;
import static projekt.substratum.common.Activities.launchExternalActivity;
import static projekt.substratum.common.Activities.launchInternalActivity;
import static projekt.substratum.common.Internal.ANDROMEDA_RECEIVER;
import static projekt.substratum.common.Internal.MAIN_ACTIVITY_RECEIVER;
import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.BYPASS_SYSTEM_VERSION_CHECK;
import static projekt.substratum.common.References.ENABLE_ROOT_CHECK;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.LOGCHAR_DIR;
import static projekt.substratum.common.References.NO_THEME_ENGINE;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_N_UNROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_UNROOTED;
import static projekt.substratum.common.References.OVERLAY_UPDATE_RANGE;
import static projekt.substratum.common.References.SAMSUNG_THEME_ENGINE_N;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.Systems.checkThemeSystemModule;
import static projekt.substratum.common.Systems.checkUsagePermissions;
import static projekt.substratum.common.Systems.isSamsung;
import static projekt.substratum.common.Systems.isSamsungDevice;
import static projekt.substratum.common.commands.FileOperations.delete;
import static projekt.substratum.common.platform.ThemeManager.uninstallOverlay;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        SearchView.OnQueryTextListener {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS = 2;
    private static final int PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS = 3;
    private static final int UNINSTALL_REQUEST_CODE = 12675;
    private static final String SELECTED_TAB_ITEM = "selected_tab_item";
    public static String userInput = "";
    public static ArrayList<String> queuedUninstall;
    public static boolean instanceBasedAndromedaFailure;
    public SearchView searchView;
    public TextView actionbarContent;
    TextView actionbarTitle;
    Toolbar toolbar;
    BottomNavigationView bottomBar;
    View bottomBarShadow;
    private ActionBar supportActionBar;
    private int permissionCheck = PackageManager.PERMISSION_DENIED;
    private Dialog progressDialog;
    private SharedPreferences prefs;
    private LocalBroadcastManager localBroadcastManager;
    private KillReceiver killReceiver;
    private AndromedaReceiver andromedaReceiver;
    private Context context;
    private BottomNavigationMenuView menuView;

    /**
     * Checks whether the overlays installed are outdated or not, based on substratum version used
     * to compile each overlay
     *
     * @param context Self explanatory, bud.
     * @return True, if there are overlays outdated, which invokes a dialog to alert the user.
     */
    private static boolean checkIfOverlaysOutdated(Context context) {
        List<String> overlays = ThemeManager.listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            int current_version = Packages.getOverlaySubstratumVersion(
                    context,
                    overlays.get(i));
            if ((current_version <= OVERLAY_UPDATE_RANGE) && (current_version != 0)) {
                Log.d("OverlayOutdatedCheck",
                        "An overlay is returning " + current_version +
                                " as Substratum's version, " +
                                "this overlay is out of date, please uninstall and reinstall!");
                return true;
            }
        }
        return false;
    }

    /**
     * A special function for Samsung users, to allow for queued uninstalls of overlays rather than
     * a barrage of uninstall dialogs to overwhelm the user.
     *
     * @param activity Activity used to specify the caller
     */
    public static void uninstallMultipleAPKS(Activity activity) {
        if (Root.checkRootAccess()) {
            uninstallOverlay(activity.getApplicationContext(), MainActivity.queuedUninstall);
        } else if (!MainActivity.queuedUninstall.isEmpty()) {
            Uri packageURI = Uri.parse("package:" + MainActivity.queuedUninstall.get(0));
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
            activity.startActivityForResult(uninstallIntent, UNINSTALL_REQUEST_CODE);
        }
    }

    /**
     * Switch back to the default Android-esque actionbar
     *
     * @param title Usually for fragment changes
     */
    public void switchToStockToolbar(CharSequence title) {
        showToolbarHamburger();
        actionbarContent.setVisibility(View.GONE);
        actionbarTitle.setVisibility(View.GONE);
        if (supportActionBar != null) supportActionBar.setTitle(title);
    }

    public void switchToDefaultToolbarText() {
        showToolbarHamburger();
        if (Systems.isSamsung(context)) {
            switchToStockToolbar(getString(R.string.samsung_app_name));
        } else if (!Systems.checkOMS(context)) {
            switchToStockToolbar(getString(R.string.legacy_app_name));
        } else {
            switchToStockToolbar(getString(R.string.nav_main));
        }
    }

    public void switchToPriorityListToolbar(View.OnClickListener listener) {
        toolbar.setTitle(getString(R.string.priority_back_title));
        showToolbarBack(listener);
    }

    private void showToolbarHamburger() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void showToolbarBack(View.OnClickListener listener) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(listener);
    }

    /**
     * Transact a different fragment on top of the current view
     *
     * @param fragment Name of the fragment in projekt.substratum.fragments
     */
    private void switchFragment(String fragment) {
        if ((searchView != null) && !searchView.isIconified()) {
            searchView.setIconified(true);
        }
        if (Systems.isSamsung(context)) {
            switchToStockToolbar(getString(R.string.samsung_app_name));
        } else if (!Systems.checkOMS(context)) {
            switchToStockToolbar(getString(R.string.legacy_app_name));
        } else {
            switchToStockToolbar(getString(R.string.nav_main));
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.main, Fragment.instantiate(this, fragment));
        tx.commitAllowingStateLoss();
        supportInvalidateOptionsMenu();
    }

    /**
     * Transact to the license fragment
     *
     * @param fragment Name of the fragment in projekt.substratum.fragments
     */
    @SuppressWarnings("unused")
    private void switchFragmentToLicenses(LibsSupportFragment fragment) {
        if ((searchView != null) && !searchView.isIconified()) {
            searchView.setIconified(true);
        }
        if (Systems.isSamsung(context)) {
            switchToStockToolbar(getString(R.string.samsung_app_name));
        } else if (!Systems.checkOMS(context)) {
            switchToStockToolbar(getString(R.string.legacy_app_name));
        } else {
            switchToStockToolbar(getString(R.string.nav_main));
        }
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.main, fragment);
        tx.commitAllowingStateLoss();
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            localBroadcastManager.unregisterReceiver(killReceiver);
        } catch (Exception e) {
            // Unregistered already
        }

        if (Systems.isAndromedaDevice(context)) {
            try {
                localBroadcastManager.unregisterReceiver(andromedaReceiver);
            } catch (Exception e) {
                // Unregistered already
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        super.onCreate(savedInstanceState);

        progressDialog = new Dialog(this, R.style.SubstratumBuilder_ActivityTheme);
        progressDialog.setCancelable(false);

        if (BuildConfig.DEBUG && !isSamsungDevice(context)) {
            Log.d(SUBSTRATUM_LOG, "Substratum launched with debug mode signatures.");
        }
        MainActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.main_activity);
        actionbarContent = binding.themeCount;
        actionbarTitle = binding.activityTitle;
        toolbar = binding.toolbar;
        bottomBarShadow = binding.bottomBarShadow;
        bottomBar = binding.bottomBar;

        cleanLogCharReportsIfNecessary();
        Theming.refreshInstalledThemesPref(context);

        // Register the main app receiver to auto kill the activity
        killReceiver = new KillReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(killReceiver,
                new IntentFilter(MAIN_ACTIVITY_RECEIVER));

        if (Systems.isAndromedaDevice(context)) {
            andromedaReceiver = new AndromedaReceiver();
            localBroadcastManager.registerReceiver(andromedaReceiver,
                    new IntentFilter(ANDROMEDA_RECEIVER));
        }

        Systems.setROMVersion(context, false);
        Systems.setAndCheckOMS(context);
        Systems.setAndCheckSubstratumService(context);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setTitle("");
        }

        supportActionBar = getSupportActionBar();
        if (Systems.isSamsung(context)) {
            switchToStockToolbar(getString(R.string.samsung_app_name));
        } else if (!Systems.checkOMS(context)) {
            switchToStockToolbar(getString(R.string.legacy_app_name));
        } else {
            switchToStockToolbar(getString(R.string.nav_main));
        }

        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        menuView = (BottomNavigationMenuView) bottomBar.getChildAt(0);

        try {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView item =
                        (BottomNavigationItemView) menuView.getChildAt(i);
                //noinspection RestrictedApi
                item.setShiftingMode(false);
                // set once again checked value, so view will be updated
                //noinspection RestrictedApi
                item.setChecked(item.getItemData().isChecked());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // suppress exception
        }

        if (Systems.checkOMS(context) && !isSamsung(context)) {
            menuView.findViewById(R.id.tab_priorities).setVisibility(View.VISIBLE);
            menuView.findViewById(R.id.tab_profiles).setVisibility(View.VISIBLE);
        } else {
            menuView.findViewById(R.id.tab_priorities).setVisibility(View.GONE);
            menuView.findViewById(R.id.tab_profiles).setVisibility(View.GONE);
        }

        bottomBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.tab_themes:
                    switchFragment(ThemeFragment.class.getCanonicalName());
                    break;
                case R.id.tab_overlay_manager:
                    switchFragment(ManagerFragment.class.getCanonicalName());
                    break;
                case R.id.tab_profiles:
                    switchFragment(ProfileFragment.class.getCanonicalName());
                    break;
                case R.id.tab_priorities:
                    switchFragment(PriorityLoaderFragment.class.getCanonicalName());
                    break;
                case R.id.tab_settings:
                    switchFragment(SettingsFragment.class.getCanonicalName());
                    break;
            }

            if (item.getItemId() != R.id.tab_overlay_manager) {
                if (ManagerFragment.layoutReloader != null &&
                        !((AsyncTask) ManagerFragment.layoutReloader).isCancelled()) {
                    ((AsyncTask) ManagerFragment.layoutReloader).cancel(true);
                }
            } else {
                if (ThemeFragment.layoutReloader != null &&
                        !ThemeFragment.layoutReloader.isCancelled()) {
                    ThemeFragment.layoutReloader.cancel(true);
                }
            }
            return true;
        });
        // On configuration change, the bar will be reset, just like the fragments
        bottomBar.setSaveEnabled(false);

        if ((getIntent() != null) &&
                getIntent().getBooleanExtra("launch_manager_fragment", false)) {
            bottomBar.setSelectedItemId(R.id.tab_overlay_manager);
        } else {
            if (savedInstanceState != null) {
                bottomBar.setSelectedItemId(savedInstanceState.getInt(SELECTED_TAB_ITEM));
            } else {
                bottomBar.setSelectedItemId(R.id.tab_themes);
            }
        }

        if (Systems.checkSubstratumService(getApplicationContext()) ||
                Systems.checkThemeInterfacer(getApplicationContext())) {
            if (!Systems.authorizedToUseBackend(getApplicationContext())) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.backend_not_authorized_title)
                        .setMessage(R.string.backend_not_authorized_text)
                        .setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
                            startActivity(
                                    new Intent(ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                            finishAffinity();
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialogInterface, i) ->
                                finishAffinity())
                        .setCancelable(false)
                        .show();
            }
        }
        new RootRequester(this).execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putInt(SELECTED_TAB_ITEM, bottomBar.getSelectedItemId());
        super.onSaveInstanceState(bundle);
    }

    /**
     * Function to clean saved LogChar reports in the LogChar directory
     */
    private void cleanLogCharReportsIfNecessary() {
        Date currentDate = new Date(System.currentTimeMillis());
        if (prefs.getLong("previous_logchar_cleanup", 0L) == 0L) {
            prefs.edit().putLong("previous_logchar_cleanup", currentDate.getTime()).apply();
            return;
        }
        long lastCleanupDate = prefs.getLong("previous_logchar_cleanup", 0L);
        long diff = currentDate.getTime() - lastCleanupDate;
        if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) >= 15L) {
            prefs.edit().putLong("previous_logchar_cleanup", currentDate.getTime()).apply();
            new ClearLogs(this).execute();
            Log.d(SUBSTRATUM_LOG, "LogChar reports were wiped from the storage");
        }
    }

    /**
     * Assign actions to every option when they are selected
     *
     * @param item Object of menu item
     * @return True, if something has changed.
     */
    @SuppressWarnings("LocalCanBeFinal")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences prefs = context.getSharedPreferences(
                "substratum_state", Context.MODE_PRIVATE);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.showcase:
                launchInternalActivity(this, ShowcaseActivity.class);
                return true;
            case R.id.rescue:
                Restore.invoke(context, this);
                return true;

            // Begin OMS based options
            case R.id.per_app:
                if (!References.isServiceRunning(SubstratumFloatInterface.class, context)) {
                    if (Settings.canDrawOverlays(context) && checkUsagePermissions(context)) {
                        showFloatingHead();
                    } else if (!Settings.canDrawOverlays(context)) {
                        DialogInterface.OnClickListener dialogClickListener = (dialog,
                                                                               which) -> {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    Intent draw_over_apps = new Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:" + context
                                                    .getPackageName()));
                                    startActivityForResult(draw_over_apps,
                                            PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS);
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    dialog.dismiss();
                                    break;
                            }
                        };
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.per_app_request_title)
                                .setMessage(R.string.per_app_draw_over_other_apps_request)
                                .setPositiveButton(R.string.dialog_ok, dialogClickListener)
                                .setNegativeButton(R.string.dialog_cancel, dialogClickListener)
                                .show();
                    } else if (!checkUsagePermissions(context)) {
                        DialogInterface.OnClickListener dialogClickListener = (dialog,
                                                                               which) -> {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    Intent usage = new Intent(Settings
                                            .ACTION_USAGE_ACCESS_SETTINGS);
                                    startActivityForResult(usage,
                                            PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS);
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    dialog.dismiss();
                                    break;
                            }
                        };
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.per_app_request_title)
                                .setMessage(R.string.per_app_usage_stats_request)
                                .setPositiveButton(R.string.dialog_ok, dialogClickListener)
                                .setNegativeButton(R.string.dialog_cancel, dialogClickListener)
                                .show();
                    }
                } else {
                    hideFloatingHead();
                }
                return true;

            case R.id.restart_systemui:
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            ThemeManager.restartSystemUI(context);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.dialog_restart_systemui_title));
                builder.setMessage(getString(R.string.dialog_restart_systemui_content));
                builder.setPositiveButton(
                        getString(R.string.restore_dialog_okay), dialogClickListener);
                builder.setNegativeButton(
                        getString(R.string.dialog_cancel), dialogClickListener);
                builder.show();
                return true;
            // Begin RRO based options
            case R.id.reboot_device:
                dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            prefs.edit().clear().apply();
                            ElevatedCommands.reboot();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                };
                builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.dialog_restart_reboot_title));
                builder.setMessage(getString(R.string.dialog_restart_reboot_content));
                builder.setPositiveButton(
                        getString(R.string.restore_dialog_okay), dialogClickListener);
                builder.setNegativeButton(
                        getString(R.string.dialog_cancel), dialogClickListener);
                builder.show();
                return true;

            case R.id.soft_reboot:
                dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            prefs.edit().clear().apply();
                            ElevatedCommands.softReboot();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                };
                builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.dialog_restart_soft_reboot_title));
                builder.setMessage(getString(R.string.dialog_restart_soft_reboot_content));
                builder.setPositiveButton(
                        getString(R.string.restore_dialog_okay), dialogClickListener);
                builder.setNegativeButton(
                        getString(R.string.dialog_cancel), dialogClickListener);
                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Certain devices do not pass the same button, so we must enforce it
     *
     * @param code Code of action
     * @param e    Event
     * @return True, if it reacted the way we wanted it to
     */
    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        switch (code) {
            case KeyEvent.KEYCODE_BACK:
                onBackPressed();
                return true;
            default:
                return super.onKeyDown(code, e);
        }
    }

    /**
     * Always end the activity gracefully.
     */
    @Override
    public void onBackPressed() {
        if (ManagerFragment.materialSheetFab != null &&
                ManagerFragment.materialSheetFab.isSheetVisible()) {
            ManagerFragment.materialSheetFab.hideSheet();
        } else if (!searchView.isIconified()) {
            searchView.setIconified(true);
            if (userInput.length() > 0) {
                onQueryTextChange("");
            }
        } else {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.main);
            if (f instanceof PriorityListFragment) {
                Fragment fragment = new PriorityLoaderFragment();
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.main, fragment);
                transaction.commit();
                switchToDefaultToolbarText();
            } else {
                if (!instanceBasedAndromedaFailure) {
                    if (bottomBar.getSelectedItemId() != R.id.tab_themes) {
                        bottomBar.setSelectedItemId(R.id.tab_themes);
                    } else {
                        finish();
                    }
                } else {
                    if (bottomBar.getSelectedItemId() != R.id.tab_overlay_manager) {
                        bottomBar.setSelectedItemId(R.id.tab_overlay_manager);
                    } else {
                        finish();
                    }
                }
            }
        }
    }

    /**
     * Activate FloatUI
     */
    private void showFloatingHead() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                context);
        prefs.edit().putInt("float_tile", Tile.STATE_ACTIVE).apply();
        FloatUiTile.requestListeningState(context,
                new ComponentName(context, FloatUiTile.class));
        context.startService(new Intent(context,
                SubstratumFloatInterface.class));
        PackageManager packageManager = getPackageManager();
        ComponentName componentName =
                new ComponentName(context, FloatUiTile.class);
        packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        startService(new Intent(this, FloatUiTile.class));
    }

    /**
     * Deactivate FloatUI
     */
    private void hideFloatingHead() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                context);
        prefs.edit().putInt("float_tile", Tile.STATE_INACTIVE).apply();
        FloatUiTile.requestListeningState(context,
                new ComponentName(context, FloatUiTile.class));
        stopService(new Intent(context,
                SubstratumFloatInterface.class));
    }

    /**
     * Handle all the startActivityWithResult calls
     *
     * @param requestCode Code used to feed into activity start
     * @param resultCode  Code that was fed to this activity after succession
     * @param data        Result from the received startActivityWithResult
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS:
                if (!checkUsagePermissions(context)) {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent, PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS);
                } else {
                    if (Settings.canDrawOverlays(context) &&
                            checkUsagePermissions(context)) {
                        showFloatingHead();
                    }
                }
                break;
            case PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS:
                if (Settings.canDrawOverlays(context) &&
                        checkUsagePermissions(context)) {
                    showFloatingHead();
                }
                break;
            case UNINSTALL_REQUEST_CODE:
                if ((queuedUninstall != null) && !queuedUninstall.isEmpty()) {
                    queuedUninstall.remove(0);
                    uninstallMultipleAPKS(this);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            setIntent(intent);
            if (getIntent().getBooleanExtra("launch_manager_fragment", false)) {
                switchFragment(ManagerFragment.class.getCanonicalName());
            }
        }
    }

    /**
     * Dealing with the permissions post-Android 6.0
     *
     * @param requestCode  Code used to feed into the permission call
     * @param permissions  Permissions that were granted/ungranted
     * @param grantResults Results of the newly set user configuration
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission already granted, allow the program to continue running
                    File directory = new File(EXTERNAL_STORAGE_CACHE);
                    if (directory.exists()) {
                        boolean deleted = directory.delete();
                        if (!deleted) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to delete directory");
                    } else {
                        Log.d(References.SUBSTRATUM_LOG, "Deleting old cache dir: " + directory);
                    }
                    if (!directory.exists()) {
                        boolean made = directory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to create directory");
                    } else {
                        References.injectRescueArchives(context);
                        Log.d(References.SUBSTRATUM_LOG, "Successfully made dir: " + directory);
                    }
                    File cacheDirectory = new File(getCacheDir(),
                            SUBSTRATUM_BUILDER_CACHE);
                    if (!cacheDirectory.exists()) {
                        boolean made = cacheDirectory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to create cache directory");
                    }
                    File[] fileList = new File(getCacheDir().getAbsolutePath() +
                            SUBSTRATUM_BUILDER_CACHE).listFiles();
                    for (File file : fileList) {
                        FileOperations.delete(context, getCacheDir()
                                .getAbsolutePath() +
                                SUBSTRATUM_BUILDER_CACHE + file.getName());
                    }
                    Log.d(SUBSTRATUM_BUILDER, "The cache has been flushed!");
                    References.injectRescueArchives(context);
                } else {
                    // permission was not granted, show closing dialog
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_not_granted_dialog_title)
                            .setMessage(R.string.permission_not_granted_dialog_message1)
                            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                                if (shouldShowRequestPermissionRationale(
                                        WRITE_EXTERNAL_STORAGE)) {
                                    finish();
                                } else {
                                    // User choose not to show request again
                                    Intent i = new Intent();
                                    i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    i.addCategory(Intent.CATEGORY_DEFAULT);
                                    i.setData(Uri.parse("package:" + getPackageName()));
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(i);
                                    finish();
                                }
                            })
                            .show();
                    return;
                }
                break;
            }
        }
    }

    /**
     * When the search bar text was changed, reload the fragment
     *
     * @param query User's input
     * @return True, if the text was changed
     */
    @Override
    public boolean onQueryTextChange(String query) {
        if (!userInput.equals(query)) {
            userInput = query;
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main);
            if (currentFragment instanceof ThemeFragment ||
                    currentFragment instanceof ManagerFragment) {
                currentFragment.onConfigurationChanged(new Configuration());
            }
        }
        return true;
    }

    /**
     * When the search bar text was changed, and then the user presses enter
     *
     * @param query User's input
     * @return True, if the text was changed
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    /**
     * A class that shows an unclearable dialog on top of the app which blocks access to the app,
     * based on whether there is no root, or Andromeda mode has been disconnected.
     */
    private static class RootRequester extends AsyncTask<Void, Void, Boolean> {
        boolean isRunning = true;
        private WeakReference<MainActivity> ref;

        private RootRequester(MainActivity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        private void permissionCheck() {
            MainActivity activity = ref.get();
            if (activity != null) {
                Context context = activity.context;
                activity.permissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        WRITE_EXTERNAL_STORAGE);

                if (activity.permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setTitle(R.string.permission_explanation_title)
                            .setMessage(R.string.permission_explanation_text)
                            .setPositiveButton(R.string.accept, (dialog, i) -> {
                                dialog.cancel();

                                if (activity.permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    // permission already granted,
                                    // allow the program to continue running
                                    File directory = new File(EXTERNAL_STORAGE_CACHE);
                                    if (!directory.exists()) {
                                        boolean made = directory.mkdirs();
                                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                                "Unable to create directory");
                                    }
                                    File cacheDirectory = new File(activity.getCacheDir(),
                                            SUBSTRATUM_BUILDER_CACHE);
                                    if (!cacheDirectory.exists()) {
                                        boolean made = cacheDirectory.mkdirs();
                                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                                "Unable to create cache directory");
                                    }
                                    References.injectRescueArchives(context);
                                } else {
                                    ActivityCompat.requestPermissions(activity,
                                            new String[]{
                                                    WRITE_EXTERNAL_STORAGE},
                                            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                                }

                                if (!Systems.checkROMVersion(context)) {
                                    activity.prefs.edit().remove("oms_state").apply();
                                    Systems.setROMVersion(context, true);
                                    Systems.setAndCheckOMS(context);
                                    Systems.setAndCheckSubstratumService(context);
                                    activity.recreate();
                                }

                                if (!Systems.checkOMS(context) &&
                                        !activity.prefs.contains("legacy_dismissal")) {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                    alert.setTitle(R.string.warning_title);
                                    if (isSamsungDevice(context)) {
                                        alert.setMessage(R.string.samsung_warning_content);
                                    } else {
                                        alert.setMessage(R.string.legacy_warning_content);
                                    }
                                    alert.setPositiveButton(R.string.dialog_ok,
                                            (dialog2, i2) -> dialog2.cancel());
                                    alert.setNeutralButton(R.string.dialog_do_not_show_again,
                                            (dialog3, i3) -> {
                                                activity.prefs.edit().putBoolean(
                                                        "legacy_dismissal", true).apply();
                                                dialog3.cancel();
                                            });
                                    alert.show();
                                }

                                if (Systems.checkOMS(context) &&
                                        Systems.isXiaomiDevice(context) &&
                                        !activity.prefs.contains("xiaomi_enable_development")) {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                    alert.setTitle(R.string.warning_title);
                                    alert.setMessage(R.string.xiaomi_warning_content);
                                    alert.setPositiveButton(R.string.dialog_ok,
                                            (dialog2, i2) -> dialog2.cancel());
                                    alert.setNegativeButton(R.string.dialog_check, (dlg, which) -> {
                                        activity.startActivity(
                                                new Intent(ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                                        activity.finishAffinity();
                                    });
                                    alert.setNeutralButton(R.string.dialog_do_not_show_again,
                                            (dialog3, i3) -> {
                                                activity.prefs.edit().putBoolean(
                                                        "xiaomi_enable_development", true).apply();
                                                dialog3.cancel();
                                            });
                                    alert.show();
                                }

                                if ((checkThemeSystemModule(context) ==
                                        OVERLAY_MANAGER_SERVICE_O_ROOTED) &&
                                        !activity.prefs.contains("rooted_oms_dismissal")) {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                    alert.setTitle(R.string.rooted_oms_dialog_warning_title);
                                    alert.setMessage(R.string.rooted_oms_dialog_warning_text);
                                    alert.setPositiveButton(R.string.dialog_ok,
                                            (dialog2, i2) -> dialog2.cancel());
                                    alert.setNeutralButton(R.string.dialog_do_not_show_again,
                                            (dialog3, i3) -> {
                                                activity.prefs.edit().putBoolean(
                                                        "rooted_oms_dismissal", true).apply();
                                                dialog3.cancel();
                                            });
                                    alert.show();
                                }
                            })
                            .setNegativeButton(R.string.deny,
                                    (dialog, i) -> {
                                        dialog.cancel();
                                        activity.finish();
                                    })
                            .show();
                } else {
                    if (!Systems.checkROMVersion(context)) {
                        Systems.setROMVersion(context, true);
                        activity.prefs.edit().remove("oms_state").apply();
                        Systems.setAndCheckOMS(context);
                        Systems.setAndCheckSubstratumService(context);
                        activity.recreate();
                    }

                    if (!Systems.checkOMS(context) &&
                            !activity.prefs.contains("legacy_dismissal")) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle(R.string.warning_title);
                        if (isSamsungDevice(context)) {
                            alert.setMessage(R.string.samsung_warning_content);
                        } else {
                            alert.setMessage(R.string.legacy_warning_content);
                        }
                        alert.setPositiveButton(R.string.dialog_ok, (dialog2, i2) ->
                                dialog2.cancel());
                        alert.setNeutralButton(R.string.dialog_do_not_show_again,
                                (dialog3, i3) -> {
                                    activity.prefs.edit().putBoolean(
                                            "legacy_dismissal", true).apply();
                                    dialog3.cancel();
                                });
                        alert.show();
                    }

                    if (Systems.checkOMS(context) &&
                            Systems.isXiaomiDevice(context) &&
                            !activity.prefs.contains("xiaomi_enable_development")) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle(R.string.warning_title);
                        alert.setMessage(R.string.xiaomi_warning_content);
                        alert.setPositiveButton(R.string.dialog_ok,
                                (dialog2, i2) -> dialog2.cancel());
                        alert.setNegativeButton(R.string.dialog_check, (dialog, which) -> {
                            activity.startActivity(
                                    new Intent(ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                            activity.finishAffinity();
                        });
                        alert.setNeutralButton(R.string.dialog_do_not_show_again,
                                (dialog3, i3) -> {
                                    activity.prefs.edit().putBoolean(
                                            "xiaomi_enable_development", true).apply();
                                    dialog3.cancel();
                                });
                        alert.show();
                    }

                    if ((checkThemeSystemModule(context) ==
                            OVERLAY_MANAGER_SERVICE_O_ROOTED) &&
                            !activity.prefs.contains("rooted_oms_dismissal")) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle(R.string.rooted_oms_dialog_warning_title);
                        alert.setMessage(R.string.rooted_oms_dialog_warning_text);
                        alert.setPositiveButton(R.string.dialog_ok,
                                (dialog2, i2) -> dialog2.cancel());
                        alert.setNeutralButton(R.string.dialog_do_not_show_again,
                                (dialog3, i3) -> {
                                    activity.prefs.edit().putBoolean(
                                            "rooted_oms_dismissal", true).apply();
                                    dialog3.cancel();
                                });
                        alert.show();
                    }

                    Broadcasts.startKeyRetrievalReceiver(context);
                }
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void onPostExecute(Boolean dialogReturnBool) {
            // These are hardcoded booleans and lint is squawking because
            // of the hardcode. The utility of these is mentioned alongside
            // their declarations.
            // Only enable these on debug builds
            if (BuildConfig.DEBUG)
                dialogReturnBool &= ENABLE_ROOT_CHECK & !BYPASS_SYSTEM_VERSION_CHECK;

            super.onPostExecute(dialogReturnBool);
            MainActivity activity = ref.get();
            if (activity != null) {
                Context context = activity.context;
                showDialogOrNot(dialogReturnBool);
                if (!dialogReturnBool) permissionCheck();
                if (checkIfOverlaysOutdated(context)) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.overlays_outdated)
                            .setMessage(R.string.overlays_outdated_message)
                            .setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
                            })
                            .show();
                }
            }
        }

        private void showDialogOrNot(boolean passthrough) {
            MainActivity activity = ref.get();
            isRunning = false;
            if (activity != null) {
                Context context = activity.context;
                if (passthrough) {
                    activity.progressDialog.show();
                    activity.progressDialog.setContentView(R.layout.main_activity_attention_dialog);

                    TextView titleView = activity.progressDialog.findViewById(R.id.title);
                    TextView textView =
                            activity.progressDialog.findViewById(R.id.root_rejected_text);
                    if (isSamsungDevice(context)) {
                        TextView samsungTitle = activity.progressDialog.findViewById(
                                R.id.sungstratum_title);
                        Button samsungButton = activity.progressDialog.findViewById(
                                R.id.sungstratum_button);
                        samsungButton.setOnClickListener(view ->
                                launchActivityUrl(context, R.string.sungstratum_url));
                        if (!FirebaseAnalytics.checkFirebaseAuthorized()) {
                            samsungTitle.setText(activity.getString(
                                    R.string.samsung_prototype_no_firebase_dialog));
                        } else if (Packages.isPackageInstalled(context, SST_ADDON_PACKAGE)) {
                            samsungTitle.setText(
                                    activity.getString(
                                            R.string.samsung_prototype_reinstall_dialog));
                            samsungButton.setVisibility(View.VISIBLE);
                        } else {
                            samsungButton.setVisibility(View.VISIBLE);
                        }
                        samsungTitle.setVisibility(View.VISIBLE);
                        textView.setVisibility(View.GONE);
                        titleView.setVisibility(View.GONE);
                    } else if (Systems.isAndromedaDevice(context) &&
                            !AndromedaService.checkServerActivity()) {
                        TextView andromedaTitle = activity.progressDialog.findViewById(
                                R.id.andromeda_title);
                        Button andromedaOfflineButton =
                                activity.progressDialog.findViewById(R.id.andromeda_offline_button);
                        TextView andromedaDebugText =
                                activity.progressDialog.findViewById(R.id.andromeda_debug_text);
                        andromedaTitle.setText(R.string.andromeda_disconnected);
                        andromedaTitle.setVisibility(View.VISIBLE);
                        Button andromedaButton = activity.progressDialog.findViewById(
                                R.id.andromeda_button);
                        andromedaButton.setText(R.string.andromeda_check_status);
                        andromedaButton.setVisibility(View.VISIBLE);
                        andromedaButton.setOnClickListener(view ->
                                launchExternalActivity(context, ANDROMEDA_PACKAGE, "InfoActivity"));
                        andromedaOfflineButton.setVisibility(View.VISIBLE);
                        andromedaOfflineButton.setOnClickListener(v ->
                                activity.progressDialog.cancel());
                        if (BuildConfig.DEBUG) {
                            andromedaDebugText.setVisibility(View.VISIBLE);
                        }

                        textView.setVisibility(View.GONE);
                        titleView.setVisibility(View.GONE);
                        instanceBasedAndromedaFailure = true;
                        activity.menuView.findViewById(R.id.tab_themes).setVisibility(View.GONE);
                        activity.menuView.findViewById(R.id.tab_priorities).setVisibility(View.GONE);
                        activity.menuView.findViewById(R.id.tab_profiles).setVisibility(View.GONE);
                        activity.bottomBar.setSelectedItemId(R.id.tab_overlay_manager);
                    } else if (Systems.checkOreo() &&
                            !Packages.isPackageInstalled(context, ANDROMEDA_PACKAGE)) {
                        TextView andromedaTitle = activity.progressDialog.findViewById(
                                R.id.andromeda_title);
                        andromedaTitle.setVisibility(View.VISIBLE);
                        Button andromedaButton = activity.progressDialog.findViewById(
                                R.id.andromeda_button);
                        andromedaButton.setVisibility(View.VISIBLE);
                        andromedaButton.setOnClickListener(view ->
                                launchActivityUrl(context, R.string.andromeda_url));
                        textView.setVisibility(View.GONE);
                        titleView.setVisibility(View.GONE);
                    }
                } else {
                    BinaryInstaller.install(activity.context, false);
                    if (Systems.checkOMS(context)) new DoCleanUp(context).execute();
                }
            }
        }

        @Override
        protected Boolean doInBackground(Void... sUrl) {
            MainActivity activity = ref.get();
            if (activity != null) {
                Context context = activity.context;
                int themeSystemModule = checkThemeSystemModule(context, true);

                // Samsung mode, but what if package is not installed?
                boolean samsungCheck = themeSystemModule == SAMSUNG_THEME_ENGINE_N;
                if (samsungCheck) {
                    // Throw the dialog when sungstratum addon is not installed
                    return !Packages.isPackageInstalled(context, SST_ADDON_PACKAGE) ||
                            !isSamsung(context);
                }

                // Check if the system is Andromeda mode
                boolean andromedaCheck = themeSystemModule == OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
                if (andromedaCheck) {
                    // Throw the dialog when checkServerActivity() isn't working
                    if (!AndromedaService.checkServerActivity()) {
                        Log.e(SUBSTRATUM_LOG,
                                "AndromedaService binder lookup failed, " +
                                        "scheduling immediate restart...");
                        context.stopService(new Intent(context, AndromedaBinderService.class));
                        if (!instanceBasedAndromedaFailure) {
                            Substratum.getInstance()
                                    .startBinderService(AndromedaBinderService.class);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return !AndromedaService.checkServerActivity();
                }

                // Check for Substratum Service
                boolean ssCheck = Systems.checkSubstratumService(context);
                if (ssCheck) {
                    return (themeSystemModule != OVERLAY_MANAGER_SERVICE_O_UNROOTED);
                }

                // Check for OMS
                boolean omsCheck = Systems.checkOMS(context);
                if (omsCheck) {
                    return (themeSystemModule != OVERLAY_MANAGER_SERVICE_O_UNROOTED) &&
                            (themeSystemModule != OVERLAY_MANAGER_SERVICE_N_UNROOTED) &&
                            !Root.requestRootAccess();
                }

                // Check if the system is legacy
                boolean legacyCheck = themeSystemModule == NO_THEME_ENGINE;
                if (legacyCheck) {
                    // Throw the dialog, after checking for root
                    return !Root.requestRootAccess();
                }
            }
            return false;
        }
    }

    /**
     * A class that specializes in cleaning up overlays when the target is not found, for example,
     * if Facebook was uninstalled, the overlays for Facebook will automatically uninstall.
     * <p>
     * This will not work for Samsung or RRO Legacy as the intended purpose allows.
     */
    public static class DoCleanUp extends AsyncTask<Void, Void, Void> {
        private WeakReference<Context> ref;

        public DoCleanUp(Context context) {
            super();
            ref = new WeakReference<>(context);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            Context context = ref.get();
            if (context != null) {
                ArrayList<String> removeList = new ArrayList<>();
                // Overlays with non-existent targets
                List<String> state1 = ThemeManager.listOverlays(
                        context, ThemeManager.STATE_MISSING_TARGET);
                // Uninstall overlays when the main theme is not present,
                // regardless if enabled/disabled
                List<String> stateAll = ThemeManager.listAllOverlays(context);
                // We need the null check because listOverlays never returns null, but empty
                if (!state1.isEmpty() && (state1.get(0) != null)) {
                    for (int i = 0; i < state1.size(); i++) {
                        Log.e("OverlayCleaner",
                                "Target APK not found for \"" + state1.get(i) +
                                        "\" and will be removed.");
                        removeList.add(state1.get(i));
                    }
                }

                for (int i = 0; i < stateAll.size(); i++) {
                    String parent = Packages.getOverlayParent(context, stateAll.get(i));
                    if (parent != null) {
                        if (!Packages.isPackageInstalled(context, parent)) {
                            Log.e("OverlayCleaner",
                                    "Parent APK not found for \"" + stateAll.get(i) +
                                            "\" and will be removed.");
                            removeList.add(stateAll.get(i));
                        }
                    }
                }

                if (!removeList.isEmpty())
                    uninstallOverlay(context, removeList);
            }
            return null;
        }
    }

    /**
     * Accompaniment function for {@link #cleanLogCharReportsIfNecessary()}
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class ClearLogs extends AsyncTask<Void, Void, Void> {
        private WeakReference<MainActivity> ref;

        ClearLogs(MainActivity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            MainActivity activity = ref.get();
            if (activity != null) {
                Context context = activity.context;
                delete(context, new File(LOGCHAR_DIR).getAbsolutePath());
            }
            return null;
        }
    }

    /**
     * Receiver to kill the activity
     */
    class KillReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

    /**
     * Receiver to close all the stacked activities and show the RootRequester dialog with an
     * Andromeda warning to connect to the PC.
     */
    class AndromedaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!progressDialog.isShowing()) {
                RootRequester rootRequester = new RootRequester(MainActivity.this);
                rootRequester.execute();
            }
        }
    }
}
