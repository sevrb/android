package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.SMSVerificationActivity;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.listeners.GetUserDataListener;
import mega.privacy.android.app.listeners.ResetPhoneNumberListener;
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.adapters.LastContactsAdapter;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity;
import mega.privacy.android.app.modalbottomsheet.PhoneNumberBottomSheetDialogFragment;
import mega.privacy.android.app.utils.TextUtil;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaUser;

import static android.graphics.Color.WHITE;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.utils.AlertsAndWarnings.dismissAlertDialogIfShown;
import static mega.privacy.android.app.utils.AlertsAndWarnings.isAlertDialogShown;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.showChangeApiServerDialog;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class MyAccountFragmentLollipop extends Fragment implements OnClickListener, GetUserDataListener.OnUserDataUpdateCallback, ResetPhoneNumberListener.OnResetPhoneNumberCallback {
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	private static final String KEY_RPN_DIALOG_SHOWING = "removePhoneNumberDialogShowing";
	private static final String IS_CHANGE_API_SERVER_SHOWN = "IS_CHANGE_API_SERVER_SHOWN";

	private final int WIDTH = 500;

	private ScrollView scrollView;
	private Context context;
	private MyAccountInfo myAccountInfo;

	private RelativeLayout avatarLayout;
	private RoundedImageView myAccountImage;

	private EmojiTextView nameView;

	private TextView typeAccount;
	private TextView infoEmail;
	private TextView expiryRenewText;
	private TextView expiryRenewDate;
	private TextView lastSession;
	private TextView connections;
	private TextView addPhoneNumber;

	private ImageView editImageView;

	private Button upgradeButton;
	private Button logoutButton;
	private Button mkButton;
	private Button changePassButton;

	private LinearLayout accountTypeLayout;
	private LinearLayout accountTypeSeparator;
	private LinearLayout expiryRenewLayout;
	private LinearLayout expiryRenewSeparator;
	private LinearLayout typeLayout;
	private LinearLayout lastSessionLayout;
    private RelativeLayout connectionsLayout;
    private CustomizedGridRecyclerView lastContactsGridView;

	private LinearLayout achievementsLayout;
	private LinearLayout achievementsSeparator;

	private LinearLayout parentLinearLayout;

	private LinearLayout businessAccountManagementAlert;
	private LinearLayout businessAccountContainer;
	private TextView businessAccountTypeText;
	private LinearLayout businessAccountStatusContainer;
	private LinearLayout businessAccountStatusSeparator;
	private TextView businessAccountStatusText;
	private TextView businessAccountRenewsText;
	private TextView businessAccountRenewsDateText;


	private ArrayList<MegaUser> lastContacted;
	
	private DisplayMetrics outMetrics;

	private MegaApiAndroid megaApi;
	private MegaChatApiAndroid megaChatApi;

	private Bitmap qrAvatarSave;

	private int numOfClicksLastSession = 0;

	private DatabaseHandler dbH;

	private TextView logoutWarning;

	private AlertDialog removePhoneNumberDialog;

	private boolean removePhoneNumberDialogShowing;

	private PhoneNumberBottomSheetDialogFragment phoneNumberBottomSheet;

	private AlertDialog changeApiServerDialog;

    /**
     * Modify or remove verified phone number.
     */
	private boolean isModify;

    private static final String KEY_IS_MODIFY = "isModify";

	@Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);

		super.onCreate(savedInstanceState);
	}

	public void checkScroll () {
		if (scrollView != null) {
			if (scrollView.canScrollVertically(-1)) {
				((ManagerActivityLollipop) context).changeAppBarElevation(true);
			}
			else {
				((ManagerActivityLollipop) context).changeAppBarElevation(false);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if(megaChatApi==null){
			megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		View v = inflater.inflate(R.layout.fragment_my_account, container, false);

		scrollView = v.findViewById(R.id.my_account_complete_relative_layout);
		new ListenScrollChangesHelper().addViewToListen(scrollView, new ListenScrollChangesHelper.OnScrollChangeListenerCompat() {
			@Override
			public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
				checkScroll();
			}
		});
		
		if(megaApi.getMyUser() == null){
			return null;
		}

		logDebug("My user handle string: " + megaApi.getMyUserHandle());

		avatarLayout = v.findViewById(R.id.my_account_relative_layout_avatar);
		avatarLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.avatar_qr_background));
		avatarLayout.setOnClickListener(this);

		boolean createLink = true;

		if (savedInstanceState != null) {
			byte[] avatarByteArray = savedInstanceState.getByteArray("qrAvatar");
			if (avatarByteArray != null) {
				logDebug("savedInstanceState avatarByteArray != null");
				qrAvatarSave = BitmapFactory.decodeByteArray(avatarByteArray, 0, avatarByteArray.length);
				if (qrAvatarSave != null) {
					logDebug("savedInstanceState qrAvatarSave != null");
					avatarLayout.setBackground(new BitmapDrawable(qrAvatarSave));
					createLink = false;
				}
			}
            if (savedInstanceState.getBoolean(KEY_RPN_DIALOG_SHOWING, false)) {
                showConfirmRemovePhoneNumberDialog(isModify = savedInstanceState.getBoolean(KEY_IS_MODIFY, false));
            }

            if (savedInstanceState.getBoolean(IS_CHANGE_API_SERVER_SHOWN, false)) {
				changeApiServerDialog =
						showChangeApiServerDialog((ManagerActivityLollipop) context, megaApi);
			}
		}

		if (createLink) {
			megaApi.contactLinkCreate(false, (ManagerActivityLollipop) context);
		}

		nameView = v.findViewById(R.id.my_account_name);
		nameView.setOnClickListener(this);

		editImageView = (ImageView) v.findViewById(R.id.my_account_edit_icon);

		infoEmail = v.findViewById(R.id.my_account_email);
		infoEmail.setText(megaApi.getMyEmail());

		myAccountImage = v.findViewById(R.id.my_account_thumbnail);

        String registeredPhoneNumber = megaApi.smsVerifiedPhoneNumber();
        addPhoneNumber = v.findViewById(R.id.add_phone_number);
        addPhoneNumber.setOnClickListener(this);
        if (!TextUtil.isTextEmpty(registeredPhoneNumber)) {
            addPhoneNumber.setText(registeredPhoneNumber);
        } else if (canVoluntaryVerifyPhoneNumber()) {
            addPhoneNumber.setText(R.string.add_phone_number_label);
        } else {
            addPhoneNumber.setVisibility(View.GONE);
        }

		mkButton = v.findViewById(R.id.MK_button);
		mkButton.setOnClickListener(this);

		setMkButtonText();

		changePassButton = v.findViewById(R.id.change_pass_button);
		changePassButton.setOnClickListener(this);

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			nameView.setMaxWidthEmojis(scaleWidthPx(250, outMetrics));
			infoEmail.setMaxWidth(scaleWidthPx(250, outMetrics));
		}
		else{
			nameView.setMaxWidthEmojis(scaleWidthPx(180, outMetrics));
			infoEmail.setMaxWidth(scaleWidthPx(200, outMetrics));
		}

		accountTypeLayout = v.findViewById(R.id.my_account_account_type_layout_container);
		accountTypeSeparator = v.findViewById(R.id.my_account_type_separator);

		expiryRenewLayout = v.findViewById(R.id.expiry_renew_layout);
		expiryRenewSeparator = v.findViewById(R.id.expiry_renew_separator);
		expiryRenewText = v.findViewById(R.id.expiry_renew_text);
		expiryRenewDate = v.findViewById(R.id.expiry_renew_date);

		typeLayout = v.findViewById(R.id.my_account_account_type_layout);
		typeAccount = v.findViewById(R.id.my_account_account_type_text);

		upgradeButton = v.findViewById(R.id.my_account_account_type_button);

		upgradeButton.setText(getString(R.string.my_account_upgrade_pro));
		upgradeButton.setOnClickListener(this);

		businessAccountManagementAlert = v.findViewById(R.id.business_account_management_container);
		businessAccountContainer = v.findViewById(R.id.business_account_container);
		businessAccountTypeText = v.findViewById(R.id.business_account_type_text);
		businessAccountStatusContainer = v.findViewById(R.id.business_account_status_container);
		businessAccountStatusSeparator = v.findViewById(R.id.business_account_status_separator);
		businessAccountStatusText = v.findViewById(R.id.business_account_status_text);
		businessAccountRenewsText = v.findViewById(R.id.business_account_renews_on_label);
		businessAccountRenewsDateText = v.findViewById(R.id.business_account_renews_date_text);

		achievementsLayout = v.findViewById(R.id.my_account_achievements_layout);
		achievementsLayout.setOnClickListener(this);
		achievementsSeparator = v.findViewById(R.id.my_account_achievements_separator);

		lastSessionLayout = v.findViewById(R.id.my_account_last_session_layout);
		lastSessionLayout.setOnClickListener(this);
		lastSession = v.findViewById(R.id.my_account_last_session);

		connectionsLayout = v.findViewById(R.id.my_account_connections_layout);
		connections = v.findViewById(R.id.my_account_connections);

		logoutButton = v.findViewById(R.id.logout_button);
		logoutButton.setOnClickListener(this);

		parentLinearLayout = v.findViewById(R.id.parent_linear_layout);

		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		if((myAccountInfo.getFullName()!=null) && (!myAccountInfo.getFullName().isEmpty())){
			logDebug("MyName is: " + myAccountInfo.getFullName());
			nameView.setText(myAccountInfo.getFullName());
		}

		this.updateAvatar(true);

		updateContactsCount();

		lastContacted = getLastContactedUsers(context);
		//Draw contact's connection component if lastContacted.size > 0
        lastContactsGridView = v.findViewById(R.id.last_contacts_gridview);
        
        lastContactsGridView.setColumnCount(LastContactsAdapter.MAX_COLUMN);
        lastContactsGridView.setClipToPadding(false);
        lastContactsGridView.setHasFixedSize(false);
        
        lastContactsGridView.setAdapter(new LastContactsAdapter(getActivity(),lastContacted));

        logoutWarning = v.findViewById(R.id.logout_warning_text);
        checkLogoutWarnings();

		setAccountDetails();

		return v;
	}

	private void scrollToTop() {
		if (scrollView != null) {
			scrollView.post(new Runnable() {
				@Override
				public void run() {
					scrollView.fullScroll(View.FOCUS_UP);
				}
			});
		}
	}
    
    @Override
    public void onResume() {
        super.onResume();

        //Refresh
		megaApi.contactLinkCreate(false, (ManagerActivityLollipop) context);
		updateView();
		checkLogoutWarnings();
    }
    /**
     * Update last contacts list and refresh last contacts' avatar.
     */
    public void updateView() {

        if(lastContactsGridView!=null){
			lastContacted = getLastContactedUsers(context);
			lastContactsGridView.setAdapter(new LastContactsAdapter(getActivity(),lastContacted));
		}
    }

    public void updateContactsCount(){
		logDebug("updateContactsCounts");
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		ArrayList<MegaUser> visibleContacts=new ArrayList<MegaUser>();

		for (int i=0;i<contacts.size();i++){
			logDebug("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}
		connections.setText(getQuantityString(R.plurals.general_selection_num_contacts,
						visibleContacts.size(), visibleContacts.size()));
	}

	public void setMkButtonText(){
		logDebug("setMkButtonText");
		String mkButtonText= getString(R.string.action_export_master_key);

		if(mkButtonText.length()>27){
			boolean found = false;
			int mid = mkButtonText.length()/2;

			for(int i=mid;i<mkButtonText.length()-1;i++){
				char letter = mkButtonText.charAt(i);
				if(letter == ' '){
					StringBuilder sb = new StringBuilder(mkButtonText);
					sb.setCharAt(i, '\n');
					mkButtonText = sb.toString();
					found = true;
					break;
				}
			}

			if(!found){
				for(int i=0;i<mid;i++){
					char letter = mkButtonText.charAt(i);
					if(letter == ' '){
						StringBuilder sb = new StringBuilder(mkButtonText);
						sb.setCharAt(i, '\n');
						mkButtonText = sb.toString();
						break;
					}
				}
			}
		}

		mkButton.setText(mkButtonText);
	}

	public static MyAccountFragmentLollipop newInstance() {
		logDebug("newInstance");
		MyAccountFragmentLollipop fragment = new MyAccountFragmentLollipop();
		return fragment;
	}

	private void permitEditNameAndEmail() {
		nameView.setOnClickListener(this);
		infoEmail.setOnClickListener(this);
		editImageView.setVisibility(View.VISIBLE);
		editImageView.setOnClickListener(this);
	}

	public void setAccountDetails() {

		if (getActivity() == null || !isAdded()) {
			logWarning("Fragment MyAccount NOT Attached!");
			return;
		}

		if (myAccountInfo.getLastSessionFormattedDate() == null
				|| myAccountInfo.getLastSessionFormattedDate().trim().length() <= 0) {
			lastSession.setText(getString(R.string.recovering_info));
		} else {
			lastSession.setText(myAccountInfo.getLastSessionFormattedDate());
		}

		if (megaApi.isBusinessAccount()) {
			accountTypeLayout.setVisibility(View.GONE);
			accountTypeSeparator.setVisibility(View.GONE);
			achievementsLayout.setVisibility(View.GONE);
			achievementsSeparator.setVisibility(View.GONE);
			businessAccountContainer.setVisibility(View.VISIBLE);
			expiryRenewLayout.setVisibility(View.GONE);
			expiryRenewSeparator.setVisibility(View.GONE);

			if (megaApi.isMasterBusinessAccount()) {
				businessAccountManagementAlert.setVisibility(View.VISIBLE);
				permitEditNameAndEmail();
				businessAccountStatusContainer.setVisibility(View.VISIBLE);
				businessAccountStatusSeparator.setVisibility(View.VISIBLE);
				businessAccountTypeText.setText(R.string.admin_label);

				int status = megaApi.getBusinessStatus();

				switch (status) {
					case BUSINESS_STATUS_EXPIRED:
						status = R.string.payment_overdue_label;
						businessAccountStatusText.setTextColor(getResources().getColor(R.color.red_800));
						businessAccountRenewsDateText.setTextColor(getResources().getColor(R.color.grey_054_white_054));
						break;
					case BUSINESS_STATUS_ACTIVE:
						status = R.string.active_label;
						businessAccountStatusText.setTextColor(getResources().getColor(R.color.grey_087_white_087));
						businessAccountRenewsDateText.setTextColor(getResources().getColor(R.color.grey_087_white_087));
						break;
					case BUSINESS_STATUS_GRACE_PERIOD:
						status = R.string.payment_required_label;
						businessAccountStatusText.setTextColor(getResources().getColor(R.color.orange_400_orange_300));
						businessAccountRenewsDateText.setTextColor(getResources().getColor(R.color.orange_400_orange_300));
						break;
				}
				businessAccountStatusText.setText(status);

				if (myAccountInfo.getSubscriptionRenewTime() > 0) {
					businessAccountRenewsText.setVisibility(View.VISIBLE);
					businessAccountRenewsDateText.setVisibility(View.VISIBLE);
					businessAccountRenewsDateText.setText(formatDate(myAccountInfo.getSubscriptionRenewTime(), DATE_MM_DD_YYYY_FORMAT));
				} else {
					businessAccountRenewsText.setVisibility(View.GONE);
					businessAccountRenewsDateText.setVisibility(View.GONE);
				}
			} else {
				businessAccountManagementAlert.setVisibility(View.GONE);
				nameView.setOnClickListener(null);
				infoEmail.setOnClickListener(null);
				editImageView.setVisibility(View.GONE);

				businessAccountStatusContainer.setVisibility(View.GONE);
				businessAccountStatusSeparator.setVisibility(View.GONE);
				businessAccountTypeText.setText(R.string.user_label);
			}

			scrollToTop();
			return;
		}

		businessAccountManagementAlert.setVisibility(View.GONE);
		accountTypeLayout.setVisibility(View.VISIBLE);
		accountTypeSeparator.setVisibility(View.VISIBLE);
		upgradeButton.setVisibility(View.VISIBLE);
		businessAccountContainer.setVisibility(View.GONE);

		permitEditNameAndEmail();

		if (megaApi.isAchievementsEnabled()) {
			achievementsLayout.setVisibility(View.VISIBLE);
			achievementsSeparator.setVisibility(View.VISIBLE);
		} else {
			achievementsLayout.setVisibility(View.GONE);
			achievementsSeparator.setVisibility(View.GONE);
		}

		if (myAccountInfo.getAccountType() < 0 || myAccountInfo.getAccountType() > 4) {
			typeAccount.setText(getString(R.string.recovering_info));
		} else {
			switch (myAccountInfo.getAccountType()) {
				case 0:
					typeAccount.setText(R.string.free_account);
					break;

				case 1:
					typeAccount.setText(getString(R.string.pro1_account));
					break;

				case 2:
					typeAccount.setText(getString(R.string.pro2_account));
					break;

				case 3:
					typeAccount.setText(getString(R.string.pro3_account));
					break;

				case 4:
					typeAccount.setText(getString(R.string.prolite_account));
					break;
			}
		}

		if (myAccountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_VALID
				&& myAccountInfo.getSubscriptionRenewTime() > 0) {
			expiryRenewLayout.setVisibility(View.VISIBLE);
			expiryRenewSeparator.setVisibility(View.VISIBLE);
			expiryRenewText.setText(getString(R.string.renews_on));
			expiryRenewDate.setText(formatDate(myAccountInfo.getSubscriptionRenewTime(), DATE_MM_DD_YYYY_FORMAT));
		} else if (myAccountInfo.getProExpirationTime() > 0) {
			expiryRenewLayout.setVisibility(View.VISIBLE);
			expiryRenewSeparator.setVisibility(View.VISIBLE);
			expiryRenewText.setText(getString(R.string.expires_on));
			expiryRenewDate.setText(formatDate(myAccountInfo.getProExpirationTime(), DATE_MM_DD_YYYY_FORMAT));
		} else {
			expiryRenewLayout.setVisibility(View.GONE);
			expiryRenewSeparator.setVisibility(View.GONE);
		}

		scrollToTop();
	}

	@Override
	public void onAttach(Activity activity) {
		logDebug("onAttach");
		super.onAttach(activity);
		context = activity;
	}

	@Override
	public void onAttach(Context context) {
		logDebug("onAttach context");
		super.onAttach(context);
		this.context = context;
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");
		switch (v.getId()) {

			case R.id.logout_button:{
				logDebug("Logout button");
				((ManagerActivityLollipop) context).setPasswordReminderFromMyAccount(true);
				megaApi.shouldShowPasswordReminderDialog(true, (ManagerActivityLollipop) context);
				break;
			}
			case R.id.my_account_relative_layout_avatar:{
				logDebug("Click layout avatar");
				((ManagerActivityLollipop)context).showMyAccountOptionsPanel();
				break;
			}
			case R.id.my_account_name:
			case R.id.my_account_email:
			case R.id.my_account_edit_icon:{
				logDebug("Click user attributes text");
				((ManagerActivityLollipop)context).showDialogChangeUserAttribute();
				break;
			}
			case R.id.MK_button:{
				logDebug("Master Key button");
				((ManagerActivityLollipop)context).showMKLayout();
				break;
			}

			case R.id.change_pass_button:{
				logDebug("Change pass button");

				Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
				startActivity(intent);
				break;
			}
			case R.id.my_account_account_type_button:{
				logDebug("Upgrade Account button");
				((ManagerActivityLollipop) context).setAccountFragmentPreUpgradeAccount(MY_ACCOUNT_FRAGMENT);
				((ManagerActivityLollipop)context).showUpAF();
				break;
			}
			case R.id.my_account_achievements_layout:{
				logDebug("Show achievements");

				if(!isOnline(context)){
					((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				}
				else{
					Intent intent = new Intent(context, AchievementsActivity.class);
//				intent.putExtra("orderGetChildren", orderGetChildren);
					startActivity(intent);
				}
				break;
			}
			case R.id.my_account_last_session_layout:{
				numOfClicksLastSession++;

				if (numOfClicksLastSession < 5) {
					break;
				}

				numOfClicksLastSession = 0;
				changeApiServerDialog =
						showChangeApiServerDialog((ManagerActivityLollipop) context, megaApi);
				break;
			}
            case R.id.add_phone_number:{
                if (canVoluntaryVerifyPhoneNumber()) {
                    startActivity(new Intent(context, SMSVerificationActivity.class));
                } else if (!isBottomSheetDialogShown(phoneNumberBottomSheet) && getActivity() != null) {
                    phoneNumberBottomSheet = new PhoneNumberBottomSheetDialogFragment();
                    phoneNumberBottomSheet.show(getActivity().getSupportFragmentManager(), phoneNumberBottomSheet.getTag());
                }
                break;
            }
		}
	}

    public void showConfirmRemovePhoneNumberDialog(boolean isModify) {
        this.isModify = isModify;
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(isModify ? R.string.title_modify_phone_number : R.string.title_remove_phone_number)
                .setMessage(isModify ? R.string.modify_phone_number_message : R.string.remove_phone_number_message)
                .setPositiveButton(R.string.general_ok, (dialog, which) -> {
                    addPhoneNumber.setClickable(false);
                    removePhoneNumberDialogShowing = false;
                    dialog.dismiss();
                    resetPhoneNumber();
                })
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                    removePhoneNumberDialogShowing = false;
                    dialog.dismiss();
                });
        removePhoneNumberDialog = builder.create();
        removePhoneNumberDialog.show();
        removePhoneNumberDialogShowing = true;
    }

    private void resetPhoneNumber() {
        megaApi.resetSmsVerifiedPhoneNumber(new ResetPhoneNumberListener(context, this));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissAlertDialogIfShown(removePhoneNumberDialog);
		dismissAlertDialogIfShown(changeApiServerDialog);
    }

    public int onBackPressed(){
		logDebug("onBackPressed");
		return 0;
	}

	public void updateNameView(String fullName){
		logDebug("updateNameView");

		if (nameView != null) {
			nameView.setText(fullName);
		}

		updateAvatar(false);
	}

	public void updateMailView(String newMail){
		logDebug("newMail: " + newMail);

		if (newMail != null){
			infoEmail.setText(newMail);
		}

		File avatar = buildAvatarFile(context,newMail + ".jpg");

		if (!isFileAvailable(avatar)){
			setDefaultAvatar();
		}
	}

	public void resetPass(){
		AccountController aC = new AccountController(context);
		aC.resetPass(megaApi.getMyEmail());
	}

	public void updateAvatar(boolean retry){
		logDebug("updateAvatar");
		File avatar = null;
		String contactEmail = megaApi.getMyEmail();
		if(context!=null){
			logDebug("Context is not null");
			avatar = buildAvatarFile(context,contactEmail + ".jpg");
		}
		else{
			logWarning("context is null!!!");
			if(getActivity()!=null){
				logDebug("getActivity is not null");
                avatar = buildAvatarFile(getActivity(),contactEmail + ".jpg");
			}
			else{
				logWarning("getActivity is ALSO null");
				return;
			}
		}

		if(avatar!=null){
			setProfileAvatar(avatar, retry);
		}
		else{
			setDefaultAvatar();
		}
	}

	private void setDefaultAvatar(){
		myAccountImage.setImageBitmap(getDefaultAvatar(getColorAvatar(megaApi.getMyUser()), myAccountInfo.getFullName(), AVATAR_SIZE, true));
	}

	public void setProfileAvatar(File avatar, boolean retry){
		logDebug("setProfileAvatar");

		Bitmap imBitmap = null;
		if (avatar.exists()){
			logDebug("Avatar path: " + avatar.getAbsolutePath());
			if (avatar.length() > 0){
				logDebug("My avatar exists!");
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (imBitmap == null) {
					avatar.delete();
					logDebug("Call to getUserAvatar");
					if(retry){
						logDebug("Retry!");
                        megaApi.getUserAvatar(megaApi.getMyUser(),buildAvatarFile(context,megaApi.getMyEmail()).getAbsolutePath(),(ManagerActivityLollipop)context);
                    }
					else{
						logDebug("DO NOT Retry!");
						setDefaultAvatar();
					}
				}
				else{
					logDebug("Show my avatar");
					myAccountImage.setImageBitmap(imBitmap);
				}
			}
		}else{
			logDebug("My avatar NOT exists!");
			logDebug("Call to getUserAvatar");
			if(retry){
				logDebug("Retry!");
                megaApi.getUserAvatar(megaApi.getMyUser(),buildAvatarFile(context,megaApi.getMyEmail()).getAbsolutePath(),(ManagerActivityLollipop)context);
			}
			else{
				logDebug("DO NOT Retry!");
				setDefaultAvatar();
			}
		}
	}

	public Bitmap queryQR (String contactLink) {
		logDebug("queryQR");

		Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<>();
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

		BitMatrix bitMatrix = null;

		try {
			bitMatrix = new MultiFormatWriter().encode(contactLink, BarcodeFormat.QR_CODE, 40, 40, hints);
		} catch (WriterException e) {
			e.printStackTrace();
			return null;
		}
		int w = bitMatrix.getWidth();
		int h = bitMatrix.getHeight();
		int[] pixels = new int[w * h];
		int color = ContextCompat.getColor(context, R.color.grey_400);
		float resize = 12.2f;

		Bitmap bitmap = Bitmap.createBitmap(WIDTH, WIDTH, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		int paintColor = ContextCompat.getColor(context, R.color.white_dark_grey);
		paint.setColor(paintColor);
		c.drawRect(0, 0, WIDTH, WIDTH, paint);
		paint.setColor(color);

		for (int y = 0; y < h; y++) {
			int offset = y * w;
			for (int x = 0; x < w; x++) {
				pixels[offset + x] = bitMatrix.get(x, y) ? color : WHITE;
				if (pixels[offset + x] == color){
					c.drawCircle(x*resize, y*resize, 5, paint);
				}
//				log("pixels[offset + x]: "+Integer.toString(pixels[offset + x])+ " offset+x: "+(offset+x));
			}
		}
		paint.setColor(paintColor);
		c.drawRect(3*resize, 3*resize, 11.5f*resize, 11.5f*resize, paint);
		c.drawRect(28.5f*resize, 3*resize, 37*resize, 11.5f*resize, paint);
		c.drawRect(3*resize, 28.5f*resize, 11.5f*resize, 37*resize, paint);

		paint.setColor(color);

		if (Build.VERSION.SDK_INT >= 21) {
			c.drawRoundRect(3.75f * resize, 3.75f * resize, 10.75f * resize, 10.75f * resize, 30, 30, paint);
			c.drawRoundRect(29.25f * resize, 3.75f * resize, 36.25f * resize, 10.75f * resize, 30, 30, paint);
			c.drawRoundRect(3.75f * resize, 29.25f * resize, 10.75f * resize, 36.25f * resize, 30, 30, paint);

			paint.setColor(paintColor);
			c.drawRoundRect(4.75f * resize, 4.75f * resize, 9.75f * resize, 9.75f * resize, 25, 25, paint);
			c.drawRoundRect(30.25f * resize, 4.75f * resize, 35.25f * resize, 9.75f * resize, 25, 25, paint);
			c.drawRoundRect(4.75f * resize, 30.25f * resize, 9.75f * resize, 35.25f * resize, 25, 25, paint);
		}
		else {
			c.drawRoundRect(new RectF(3.75f * resize, 3.75f * resize, 10.75f * resize, 10.75f * resize), 30, 30, paint);
			c.drawRoundRect(new RectF(29.25f * resize, 3.75f * resize, 36.25f * resize, 10.75f * resize), 30, 30, paint);
			c.drawRoundRect(new RectF(3.75f * resize, 29.25f * resize, 10.75f * resize, 36.25f * resize), 30, 30, paint);

			paint.setColor(paintColor);
			c.drawRoundRect(new RectF(4.75f * resize, 4.75f * resize, 9.75f * resize, 9.75f * resize), 25, 25, paint);
			c.drawRoundRect(new RectF(30.25f * resize, 4.75f * resize, 35.25f * resize, 9.75f * resize), 25, 25, paint);
			c.drawRoundRect(new RectF(4.75f * resize, 30.25f * resize, 9.75f * resize, 35.25f * resize), 25, 25, paint);
		}

		paint.setColor(color);
		c.drawCircle(7.25f*resize, 7.25f*resize, 17.5f, paint);
		c.drawCircle(32.75f*resize, 7.25f*resize, 17.5f, paint);
		c.drawCircle(7.25f*resize, 32.75f*resize, 17.5f, paint);

//        bitmap.setPixels(pixels, 0, w, 0, 0, w,  h);

		return bitmap;
	}

	public void initCreateQR(MegaRequest request, MegaError e){
		logDebug("initCreateQR");
		if (e.getErrorCode() == MegaError.API_OK) {
			logDebug("Contact link create LONG: " + request.getNodeHandle());
			logDebug("Contact link create BASE64: " + "https://mega.nz/C!" + MegaApiAndroid.handleToBase64(request.getNodeHandle()));

			String contactLink = "https://mega.nz/C!" + MegaApiAndroid.handleToBase64(request.getNodeHandle());
			new QRBackgroundTask().execute(contactLink);
		}
		else {
			logError("Error request.getType() == MegaRequest.TYPE_CONTACT_LINK_CREATE: " + e.getErrorString());
		}
	}

    @Override
    public void onUserDataUpdate(MegaError e) {
        addPhoneNumber.setClickable(true);
        if (e.getErrorCode() == MegaError.API_OK) {
            if (canVoluntaryVerifyPhoneNumber()) {
                addPhoneNumber.setText(R.string.add_phone_number_label);
                addPhoneNumber.setVisibility(View.VISIBLE);
                if (context instanceof ManagerActivityLollipop) {
                    ((ManagerActivityLollipop) context).showAddPhoneNumberInMenu();
                }
                if (isModify) {
                    startActivity(new Intent(context, SMSVerificationActivity.class));
                } else {
                    showSnackbar(context, getString(R.string.remove_phone_number_success));
                }
            }
        } else {
            showSnackbar(context, getString(R.string.remove_phone_number_fail));
            logWarning("Get user data for updating phone number failed: " + e.getErrorCode() + ": " + e.getErrorString());
        }
    }

    @Override
    public void onResetPhoneNumber(MegaError e) {
        /*
          Reset phone number successfully or the account has reset the phone number,
          but user data hasn't refreshed successfully need to refresh user data again.
        */
        if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT) {
            // Have to getUserData to refresh, otherwise, phone number remains previous value.
            megaApi.getUserData(new GetUserDataListener(context, this));
        } else {
            addPhoneNumber.setClickable(true);
            showSnackbar(context, getString(R.string.remove_phone_number_fail));
            logWarning("Reset phone number failed: " + e.getErrorCode() + ": " + e.getErrorString());
        }
    }

    class QRBackgroundTask extends AsyncTask<String, Void, Void> {


		@Override
		protected Void doInBackground(String... strings) {
			qrAvatarSave = queryQR(strings[0]);

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			avatarLayout.setBackground(new BitmapDrawable(qrAvatarSave));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (qrAvatarSave != null){
			logDebug("qrAvatarSave != null");
			ByteArrayOutputStream qrAvatarOutputStream = new ByteArrayOutputStream();
			qrAvatarSave.compress(Bitmap.CompressFormat.PNG, 100, qrAvatarOutputStream);
			byte[] qrAvatarByteArray = qrAvatarOutputStream.toByteArray();
			outState.putByteArray("qrAvatar", qrAvatarByteArray);
		}
		outState.putBoolean(KEY_RPN_DIALOG_SHOWING, removePhoneNumberDialogShowing);
		outState.putBoolean(KEY_IS_MODIFY, isModify);
		outState.putBoolean(IS_CHANGE_API_SERVER_SHOWN, isAlertDialogShown(changeApiServerDialog));
	}

    /**
     * Check if there is offline files and transfers.
     * If yes, show the corresponding warning text at the end of My Account section.
     * If not, hide the text.
     */
	public void checkLogoutWarnings() {
		if (logoutWarning == null) return;

		boolean existOfflineFiles = existsOffline(context);
		boolean existOutgoingTransfers = existOngoingTransfers(megaApi);
		int oldVisibility = logoutWarning.getVisibility();
		String oldText = logoutWarning.getText().toString();
		int newVisibility = View.GONE;
		String newText = "";

		if (existOfflineFiles || existOutgoingTransfers) {
			if (existOfflineFiles && existOutgoingTransfers) {
				newText = getString(R.string.logout_warning_offline_and_transfers);
			} else if (existOfflineFiles) {
				newText = getString(R.string.logout_warning_offline);
			} else if (existOutgoingTransfers) {
				newText = getString(R.string.logout_warning_transfers);
			}

			newVisibility = View.VISIBLE;
		}

		if (oldVisibility != newVisibility) {
			logoutWarning.setVisibility(newVisibility);
		}

		if (!oldText.equals(newText)) {
			logoutWarning.setText(newText);
		}
	}

	public void updateAddPhoneNumberLabel(){
        logDebug("updateAddPhoneNumberLabel");
        addPhoneNumber.setVisibility(View.INVISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(() -> {

            //work around - it takes time for megaApi.smsVerifiedPhoneNumber() to return value
            String registeredPhoneNumber = megaApi.smsVerifiedPhoneNumber();
            logDebug("updateAddPhoneNumberLabel " + registeredPhoneNumber);

            if(!TextUtil.isTextEmpty(registeredPhoneNumber)){
                addPhoneNumber.setText(registeredPhoneNumber);
                addPhoneNumber.setVisibility(View.VISIBLE);
            }
        }, 3000);

    }
}
