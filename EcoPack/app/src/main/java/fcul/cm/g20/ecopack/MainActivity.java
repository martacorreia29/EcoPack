package fcul.cm.g20.ecopack;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import fcul.cm.g20.ecopack.ui.fragments.map.MapFragment;
import fcul.cm.g20.ecopack.ui.fragments.tree.TreeFragment;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_points,
                R.id.navigation_tree,
                R.id.navigation_map,
                R.id.navigation_profile
        ).build();

        BottomNavigationView bottomNavigationView = findViewById(R.id.nav_view);

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // TODO: CHANGE THIS TO A METHOD CALL IN EACH FRAGMENT, DEPENDING ON ITS USE
        getSupportActionBar().hide();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        /*
        //TODO corrigir o erro de só está a entrar no primeiro if
        if(findViewById(R.id.fragment_markers) != null){
            FragmentManager fm = getSupportFragmentManager();
            fm.popBackStack ("tree", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        if(findViewById(R.id.fragment_info) != null){
            FragmentManager fm = getSupportFragmentManager();
            fm.popBackStack ("info", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
         */
    }
}