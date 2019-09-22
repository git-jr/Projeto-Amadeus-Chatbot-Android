package com.paradoxo.amadeus.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.paradoxo.amadeus.R;
import com.paradoxo.amadeus.activity.redesign.SimpleCallback;
import com.paradoxo.amadeus.adapter.AdapterEditaMensagem;
import com.paradoxo.amadeus.dao.MensagemDAO;
import com.paradoxo.amadeus.modelo.Mensagem;
import com.paradoxo.amadeus.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.paradoxo.amadeus.util.Util.configurarToolBarBranca;

public class ListarRespostasActivity extends AppCompatActivity {

    TextView textViewNenhumaMsgAinda;
    private int posicaoDaMensagemEmEdicao = -1;
    private AdapterEditaMensagem adapterEditaMensagem;
    private ProgressDialog progressDialogCarregandoBanco;
    private List<Mensagem> mensagens = new ArrayList<>();

    @NonNull
    @Override
    public LayoutInflater getLayoutInflater() {
        return super.getLayoutInflater();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respostas_listar);

        configurarToolBarBranca(this);

        progressDialogCarregandoBanco = ProgressDialog.show(this, getString(R.string.carregando_banco), getString(R.string.aguarde), true, false);
        textViewNenhumaMsgAinda = findViewById(R.id.nenhumaMensagemAindaTextView);

        CarregarMensagens carregarMensagens = new CarregarMensagens();
        carregarMensagens.execute();

    }

    private void configurarRecycler(TextView textViewNenhumaMsgAinda) {
        textViewNenhumaMsgAinda.setVisibility(View.INVISIBLE);
        RecyclerView recyclerView = findViewById(R.id.recycler);
        adapterEditaMensagem = new AdapterEditaMensagem(mensagens);
        recyclerView.setAdapter(adapterEditaMensagem);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        posicaoDaMensagemEmEdicao = -1;
        // O valor "-1" indica que nenhuma mensagem está em edição no momento e por isso o método "onResume" não precisa atualizar a recycler quando chamado

        adapterEditaMensagem.setOnItemClickListenerEditar(new AdapterEditaMensagem.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int pos) {
                editarMensagem(pos);
                posicaoDaMensagemEmEdicao = pos;
            }
        });

        adapterEditaMensagem.setOnLongClickListener(new AdapterEditaMensagem.OnLongClickListener() {
            @Override
            public void onLongClickListener(View view, int pos, Mensagem mensagem) {
                vibrar();
                excluirMensagem(pos);
            }
        });

        ItemTouchHelper itemTouchHelper = new
                ItemTouchHelper(new SimpleCallback(adapterEditaMensagem, ListarRespostasActivity.this));
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void vibrar() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long milliseconds = 50;
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
    }

    private void excluirMensagem(final int pos) {
        final Dialog builder = new Dialog(ListarRespostasActivity.this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.setContentView(R.layout.item_msg_excluir);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

        TextView textViewItemPergunta = builder.findViewById(R.id.conteudoPerguntaTextView);
        TextView textViewItemResposta = builder.findViewById(R.id.conteudoRespostaTextView);
        textViewItemPergunta.setText(mensagens.get(pos).getConteudo());
        textViewItemResposta.setText(mensagens.get(pos).getConteudo_resposta());


        (builder.findViewById(R.id.confirmarButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    MensagemDAO mensagemDAO = new MensagemDAO(getApplicationContext());
                    mensagemDAO.excluirResposta(mensagens.get(pos));

                    mensagens = mensagemDAO.listarRespostasCompleto();
                    adapterEditaMensagem.remover(pos);

                    meuToast(String.valueOf(getApplicationContext().getText(R.string.msg_deletada_sucesso)));

                    if (mensagens.size() == 0) {
                        meuToast(String.valueOf(getApplicationContext().getText(R.string.nenhuma_resposta_gravada)));

                    }

                } catch (Exception e) {
                    meuToast(String.valueOf(getApplicationContext().getText(R.string.erro_apagar_msg)));
                }

                builder.dismiss();
            }
        });

        (builder.findViewById(R.id.cancelarButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.cancel();
            }
        });

        builder.show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarRecyclerSeOuveAlteracao();
    }

    @SuppressLint("StaticFieldLeak")
    private void atualizarRecyclerSeOuveAlteracao() {
        if (posicaoDaMensagemEmEdicao > -1) {

            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    buscarMensagensBanco();
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    adapterEditaMensagem.atualizar(mensagens.get(posicaoDaMensagemEmEdicao), posicaoDaMensagemEmEdicao);
                    posicaoDaMensagemEmEdicao = -1;

                }
            }.execute();
        }
    }

    public void editarMensagem(int position) {
        Intent alterarRespostasActivity = new Intent(this, AlteraRespostasActivity.class);
        alterarRespostasActivity.putExtra("pergunta_selecionada", mensagens.get(position).getConteudo());
        alterarRespostasActivity.putExtra("resposta_selecionada", mensagens.get(position).getConteudo_resposta());
        alterarRespostasActivity.putExtra("id_selecionado", String.valueOf(position));
        startActivity(alterarRespostasActivity);

    }

    public void meuToast(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("StaticFieldLeak")
    public class CarregarMensagens extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            buscarMensagensBanco();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialogCarregandoBanco.dismiss();
            if (mensagens.size() > 0) {
                configurarRecycler(textViewNenhumaMsgAinda);

            } else {
                textViewNenhumaMsgAinda.setVisibility(View.VISIBLE);
            }
        }
    }

    private void buscarMensagensBanco() {
        MensagemDAO msgDAO = new MensagemDAO(getBaseContext());
        mensagens = msgDAO.listarRespostasCompleto();
    }
}
