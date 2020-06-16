package edu.ilab.covid_id.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import edu.ilab.covid_id.MapsActivity;
import edu.ilab.covid_id.R;

/**
 * Allows user to sign in to access Google Firebase with their Google (Gmail) credentials
 * TODO: Implement sign out method somewhere in the application with the following line of code:
 *      FirebaseAuth.getInstance().signOut();
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Request code assigned for starting the Google Sign in activity. When the user is done
     * and returns from the google sign in activity, the system will cross reference this code
     */
    private static final int RC_SIGN_IN = 0;

    /**
     * the activity's sign in client
     */
    private GoogleSignInClient mGoogleSignInClient;

    /**
     * handle to firebase authentication object
     */
    private FirebaseAuth mAuth;

    /**
     * Called when activity instance is created, sets google sign in options and updates UI based
     * on user login status
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Checks for existing Google Sign In account and directs UI appropriately based on log-in status
     */
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    /**
     * updates the UI to reflect whether user is logged in or not
     *      if 'account' is null, user is not signed in and we want to show the Google sign in button
     *      otherwise, the user is already signed in and we can hide the button or launch the maps activity
     * NOTE: If account is NOT null, the following methods are callable:
     *      account.getEmail(), account.getID(), account.getIdToken()
     * @param account - is either null or not
     */
    protected void updateUI(FirebaseUser account) {
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        if(account == null) {
            // setup the google sign in button
            signInButton.setSize(SignInButton.SIZE_STANDARD);
            signInButton.setVisibility(View.VISIBLE);
            signInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                }
            });
        }
        else {
            // hide sign in button
            signInButton.setVisibility(View.INVISIBLE);
            // go to maps application
            Intent goToMaps = new Intent(this, MapsActivity.class);
            startActivity(goToMaps);
        }
    }

    /**
     * Method called once user completes google sign in activity to extract the user GoogleSignInAccount
     * object (in the form of a <GoogleSignInAccount> Task)
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("LOGIN", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.d("LOGIN", "Google sign in failed", e);
            }
        }
        else {
            Log.d("LOGIN", "RC_SIGN_IN failed");
        }
    }

    /**
     *
     * @param idToken
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("LOGIN", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("LOGIN", "signInWithCredential:failure", task.getException());
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Authentication Failed.",
                                    Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            updateUI(null);
                        }
                    }
                });
    }
}