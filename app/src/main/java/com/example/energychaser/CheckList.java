package com.example.energychaser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
//import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mikiloz.fancyadapters.SuperSelectableAdapter;

import java.util.ArrayList;

public class CheckList extends AppCompatActivity {

    RecyclerView recyclerView;
    CustomAdapter adapter;
    private ArrayList<String> savedList;
    private ArrayList<String> DetectedList;
    private CountDownTimer Timer;
    private boolean isTimerRunning = false;
    private ArrayList<String> reordered;
    private ImageView imageView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_check);
////////////////////////////////////////////////////////////////////////////////////////////////////
        //We start count
        countime();

        savedList = new ArrayList<>();
        //List of objects to compare
        savedList.add("ΦΟΥΡΝΟΣ");
        savedList.add("ΨΥΓΕΙΟ");
        savedList.add("ΠΛΥΝΤΗΡΙΟ");
        savedList.add("ΚΛΙΜΑΤΙΣΤΙΚΟ");
        savedList.add("ΥΠΟΛΟΓΙΣΤΗΣ");
        savedList.add("ΟΘΟΝΗ");
        savedList.add("ΦΩΤΙΣΜΟΣ");

        // Bring the detected list in this class
        DetectedList = getIntent().getStringArrayListExtra("objectList");

////////////////////////////////////////////////////////////////////////////////////////////////////
        // Call the adapter methods
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        adapter = new CustomAdapter(DetectedList, recyclerView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

////////////////////////////////////////////////////////////////////////////////////////////////////
        ////When the order is TRUE make tha award visible
        Button finalizeButton = (Button) findViewById(R.id.finalize_button);
        finalizeButton.setOnClickListener(v -> Finilize(reordered));
        imageView = findViewById(R.id.awardgif);
        imageView.setVisibility(View.INVISIBLE);
    }
    public class CustomAdapter extends SuperSelectableAdapter<String, CustomAdapter.CustomViewHolder> {
        public CustomAdapter(ArrayList<String> items, RecyclerView recyclerView) {
            super(items, recyclerView, ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }
        @Override
        public CustomViewHolder onCreateSelectableViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_view, parent, false);
            return new CustomViewHolder(itemView);
        }
        @Override
        public void onBindViewHolder(CustomViewHolder holder, int position) {
            holder.textView.setText(DetectedList.get(position));
        }
        @Override
        public void onItemSelected(CustomViewHolder holder, int position) {
        }
        @Override
        public void onItemDeselected(CustomViewHolder holder, int position) {
        }
        @Override
        public ActionMode startActionMode() {
            return null;
        }
        @Override
        public void onSelectionUpdate(ActionMode actionMode, int i) {
        }
        @Override
        public void onMove(CustomViewHolder viewHolder, CustomViewHolder target) {
            //Toast.makeText(getBaseContext(), "Item " + viewHolder.getBindingAdapterPosition() + " moved", Toast.LENGTH_SHORT).show();
            String s = DetectedList.remove(viewHolder.getBindingAdapterPosition());
            DetectedList.add(target.getBindingAdapterPosition(), s);

            ////update the list
            int fromPosition = viewHolder.getBindingAdapterPosition();
            int toPosition = target.getBindingAdapterPosition();
            // Remove the item from the list
            String item = DetectedList.remove(fromPosition);
            // Insert the item into the new position
            if (toPosition > fromPosition) {
                toPosition--;
            }
            DetectedList.add(toPosition + 1, item);
            // Notify the adapter of the item move
            notifyItemMoved(fromPosition, toPosition + 1);

            reordered = new ArrayList<>();
            reordered.addAll(DetectedList);
        }

        @Override
        public void onSwipe(CustomViewHolder viewHolder, int direction) {
        }
        public class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            public CustomViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.textview);
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                            startDrag(CustomViewHolder.this, getLayoutPosition());
                        return true;
                    }
                });
            }
        }

    }

    public void countime(){
        long remainingTime = getIntent().getLongExtra("remainingTime", 0);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        // Start the timer
        Timer = new CountDownTimer(remainingTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int CurrentProgress = (int) ((remainingTime - millisUntilFinished) * 100 / remainingTime);
                progressBar.setProgress(CurrentProgress);
                //progressBar.setMax(100);
            }
            @Override
            public void onFinish() {
                progressBar.setProgress(100);
            }
        }.start();
        isTimerRunning = true;
    }
    //Method for start Timer automatically
    @Override
    protected void onResume() {
        super.onResume();
        // resume the timer if it is running
        if (isTimerRunning) {
            Timer.start();
        }
    }
    public static boolean areItemsInOrder(ArrayList<String> list1, ArrayList<String> list2) {
        int i = 0;
        int j = 0;
        while (i < list1.size() && j < list2.size()) {
            if (list1.get(i).equals(list2.get(j))) {
                j++;
            }
            i++;
        }
        return j == list2.size();
    }
    private void Finilize(ArrayList<String> list) {
        isTimerRunning = false;
        // Check if the list is in the right order}
        boolean areInOrder = areItemsInOrder(savedList, list);
        // Display a message and award gif
        if (areInOrder) {
            Toast.makeText(CheckList.this, "Μπράβο!!!.Τα Κατάφερες Μια Χαρά!!!", Toast.LENGTH_SHORT).show();
            imageView.setVisibility(View.VISIBLE);
            backtostart();
        }
        else{
            Toast.makeText(CheckList.this, "Την Επόμενη Φορά Θα Πάει Σίγουρα Καλύτερα!!!", Toast.LENGTH_SHORT).show();
            backtostart();
        }
    }
    public void backtostart(){
        Runnable t = () -> {
            reordered.clear();
            DetectedList.clear();
            Timer.cancel();
            startActivity(new Intent(CheckList.this, ButtonsActivity.class));
        };
        Handler h = new Handler();
        // The Runnable will be executed after the given delay time
        h.postDelayed(t, 8000); // will be delayed for 4 seconds//
    }

}
