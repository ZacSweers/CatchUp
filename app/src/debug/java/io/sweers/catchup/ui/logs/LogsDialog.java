package io.sweers.catchup.ui.logs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.widget.ListView;
import android.widget.Toast;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.util.Intents;
import java.io.File;

public final class LogsDialog extends AlertDialog {
  private final LumberYard lumberYard;
  private final LogAdapter adapter;

  private CompositeDisposable disposables;

  public LogsDialog(Context context, LumberYard lumberYard) {
    super(context);
    this.lumberYard = lumberYard;

    adapter = new LogAdapter(context);

    ListView listView = new ListView(context);
    listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
    listView.setAdapter(adapter);

    setTitle("Logs");
    setView(listView);
    setButton(BUTTON_NEGATIVE, "Close", (dialog, which) -> {
      // NO-OP.
    });
    setButton(BUTTON_POSITIVE, "Share", (dialog, which) -> {
      share();
    });
  }

  @Override protected void onStart() {
    super.onStart();

    adapter.setLogs(lumberYard.bufferedLogs());

    disposables = new CompositeDisposable();
    disposables.add(lumberYard.logs() //
        .observeOn(AndroidSchedulers.mainThread()) //
        .subscribe(adapter));
  }

  @Override protected void onStop() {
    super.onStop();
    disposables.dispose();
  }

  private void share() {
    lumberYard.save() //
        .subscribeOn(Schedulers.io()) //
        .observeOn(AndroidSchedulers.mainThread()) //
        .subscribe(new SingleObserver<File>() {
          @Override public void onSubscribe(Disposable d) {

          }

          @Override public void onSuccess(File file) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            Intents.maybeStartChooser(getContext(), sendIntent);
          }

          @Override public void onError(Throwable e) {
            Toast.makeText(getContext(), "Couldn't save the logs for sharing.", Toast.LENGTH_SHORT)
                .show();
          }
        });
  }
}
