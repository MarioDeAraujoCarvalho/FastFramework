/*
 * Informações sobre a criação do arquivo.
 * Autor: Mário de Araújo Carvalho
 * E-mail: mariodearaujocarvalho@gmail.com
 * GitHub: https://github.com/MarioDeAraujoCarvalho
 * Ano: 13/5/2017
 * Entrar em contado para maiores informações.
 */

package br.embrapa.scaffold.view.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import br.embrapa.scaffold.dao.item.ItemDAO;
import br.embrapa.scaffold.interfaces.OnItemLongClickListener;
import br.embrapa.scaffold.interfaces.OnScrollListener;
import br.embrapa.scaffold.model.Item;
import br.embrapa.scaffold.util.Util;
import br.embrapa.scaffold.util.search_tools.MaterialSearchView;
import br.embrapa.scaffold.view.activities.ItemDetailsActivity;
import br.embrapa.scaffold.R;
import br.embrapa.scaffold.adapter.ItemAdapter;
import br.embrapa.scaffold.interfaces.OnClickListener;
import br.embrapa.scaffold.view.activities.ItemActivity;

public class ItensFavoritesFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnScrollListener mEndlessListener;
    private ItemAdapter mRegistroAdapter;

    private RecyclerView mRecyclerView;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private View view;

    private Context mContext = getActivity();
    final String TAG = "TAG";

    Toolbar mToolbar;
    private MaterialSearchView mToolbarSearch;

    private ItemDAO mItemDAO;
    private boolean isActionMode;
    private int position;
    private List<Item> mListaDeItens;
    private TextView mTextoLista;
    private FloatingActionButton mBtnFloatAdd;

    public ItensFavoritesFragment() {
        // Required empty public constructor
    }

    public static ItensFavoritesFragment newInstance(String param1, String param2) {
        ItensFavoritesFragment fragment = new ItensFavoritesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_item_list_root, container, false);


        mToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        mToolbar.setTitle(getResources().getString(R.string.title_fragment_list_itens_favorites));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getActivity().getWindow();
            w.setStatusBarColor(getActivity().getResources().getColor(R.color.colorPrimaryDark));
            w.setNavigationBarColor(getActivity().getResources().getColor(R.color.colorPrimaryDark));
        }

         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mToolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary, getActivity().getTheme()));
        }else{
             //noinspection deprecation
            mToolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        mToolbarSearch = (MaterialSearchView)getActivity().findViewById(R.id.search_view);
        mToolbarSearch.setVoiceSearch(true);
        mToolbarSearch.setCursorDrawable(R.drawable.color_cursor_white);
        mToolbarSearch.setOnQueryTextListener(onQuerySearchView);
        mToolbarSearch.setOnSearchViewListener(onSearchViewListener);

        mContext = getActivity();

        mTextoLista = (TextView)view.findViewById(R.id.txt_lista_de_registros);
        mSwipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.swipeContainer);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.md_green_400, R.color.md_green_600, R.color.md_green_800);

        mItemDAO = new ItemDAO(getActivity());
        mSwipeRefreshLayout.setOnRefreshListener(onRefresh);

        mRecyclerView = (RecyclerView)view.findViewById(R.id.recycler_view);

        mBtnFloatAdd = (FloatingActionButton)view.findViewById(R.id.fab);
        mBtnFloatAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.startIntentParseOneValue(getActivity(), new ItemActivity(),"codigo",(-1));
                getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

            }
        });

        mRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(manager);
        // mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        mEndlessListener = new OnScrollListener(manager, mBtnFloatAdd) {
            @Override
            public void onLoadMore(int currentPage) {
            }

            @Override
            public void onScroll(RecyclerView recyclerView, int dx, int dy, boolean onScroll) {
            }
        };
        mRecyclerView.addOnScrollListener(mEndlessListener);
        mSwipeRefreshLayout.setOnRefreshListener(onRefresh);

        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        carregarDados();
        return view;
    }

    private SwipeRefreshLayout.OnRefreshListener onRefresh = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            mSwipeRefreshLayout.setSoundEffectsEnabled(true);
            carregarDados();
        }
    };

        private void carregarDados() {

            mSwipeRefreshLayout.setRefreshing(true);

            mItemDAO.open();
            mListaDeItens = mItemDAO.getAllItensFavorites();
            mItemDAO.close();

            if(mListaDeItens.size() > 0){
                mTextoLista.setText(getResources().getString(R.string.text_list_fragment_favoritos_com_itens));
            }else{
                mTextoLista.setText(getResources().getString(R.string.text_list_fragment_favoritos_sem_itens));
            }

            mEndlessListener.resetEndlessRecyclerView();
            /*Configurar o Adapter*/
            mRegistroAdapter = new ItemAdapter(getActivity(), mListaDeItens);
            mRegistroAdapter.setOnClickListener(onClickListener);
            mRegistroAdapter.setOnItemLongClickListener(onItemLongClickListener);

            mRecyclerView.setAdapter(mRegistroAdapter);

            mSwipeRefreshLayout.setRefreshing(false);
        }


    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v, final int position) {
            Item item = mListaDeItens.get(position);

            Intent newIntent = new Intent(getActivity(), ItemDetailsActivity.class);
            newIntent.putExtra("codigo",String.valueOf(item.getId()));
            getActivity().startActivity(newIntent);
            getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }

        @Override
        public void onItemClick(View v, int position) {
            Item item = mListaDeItens.get(position);
            //  Util.startIntentParseOneValue(getActivity(), new ArvoreActivity(),"codigo",(item.getCodigoArvore()));
            // getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }

    };

    private OnItemLongClickListener onItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public void onLongClick(View v, final int position) {

            final CharSequence[] options = getResources().getStringArray(R.array.alert_dialog_menu_options);
            //{"Visualizar","Editar","Excluír","Copiar Item","Compartilhar"};

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(""+ mListaDeItens.get(position).getNome());
            builder.setItems(options, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    final Item mItemSelecionado = mListaDeItens.get(position);

                    if(item == 0){
                        Intent newIntent = new Intent(getActivity(), ItemDetailsActivity.class);
                        newIntent.putExtra("codigo",String.valueOf(mItemSelecionado.getId()));
                        getActivity().startActivity(newIntent);
                        getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

                    } else if(item == 1){
                        Util.startIntentParseOneValue(getActivity(), new ItemActivity(),"codigo",(mItemSelecionado.getId()));
                        getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    } else if(item == 2){
                        AlertDialog.Builder janela = new AlertDialog.Builder(mContext);
                        janela.setTitle(getResources().getString(R.string.text_aviso_title));
                        janela.setMessage(getResources().getString(R.string.text_aviso_excluir_item));
                        janela.setNeutralButton(getResources().getString(R.string.text_aviso_opcao_nao),null);
                        janela.setPositiveButton(getResources().getString(R.string.text_aviso_opcao_sim),new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mItemDAO.open();
                                mItemDAO.delete(mItemSelecionado.getId());
                                mItemDAO.close();

                                dialog.dismiss();
                                onResume();
                            }
                        });
                        janela.create().show();

                    } else if(item == 3){
                        mItemDAO.open();
                        mItemSelecionado.setNome(getResources().getString(R.string.text_copia)+" "+mItemSelecionado.getNome());
                        mItemSelecionado.setDescricao(getResources().getString(R.string.text_copia)+" "+mItemSelecionado.getDescricao());
                        int idInsert = mItemDAO.insertCopy(mItemSelecionado);
                        mItemDAO.close();
                        if(idInsert != -1){
                            Util.startIntentParseOneValue(getActivity(), new ItemActivity(),"codigo",(idInsert));
                            getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                        }
                    }else if(item == 4){
                        compartilhar(mItemSelecionado);
                    }
                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        }

        @Override
        public void onItemLongClick(View v, int position) {
            Item item = mListaDeItens.get(position);
            // Util.startIntentParseOneValue(getActivity(), new ArvoreActivity(),"codigo",(item.getCodigoArvore()));
            //  getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
    };

    public void compartilhar(Item item){
        String texto = "Olá Pessoal,\n" +
                "Gostaria de Comparlihar esse item: \n" +
                "\nNome: "+ item.getNome()+
                "\nDescrição: "+ item.getDescricao()+
                "\n\n\n--\nEnviado de Fast Framework";

        Util.compartilhar(mContext, texto);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
           // Log.e("TAG1",""+mParam1);
           // Log.e("TAG2",""+mParam2);

        }
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

    }


    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        // TODO Auto-generated method stub
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragments_options, menu);

        MenuItem item = menu.findItem(R.id.menu_search_toolbar_pesquisar);
        mToolbarSearch.setMenuItem(item);

    }

    private MaterialSearchView.OnQueryTextListener onQuerySearchView = new MaterialSearchView.OnQueryTextListener() {

        @Override
        public boolean onQueryTextSubmit(String arg0) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean onQueryTextChange(String texto) {
            // TODO Auto-generated method stub
            buscarTexto(texto);
            return false;
        }
    };

    private MaterialSearchView.SearchViewListener onSearchViewListener = new MaterialSearchView.SearchViewListener() {

        @Override
        public void onSearchViewShown() {

        }

        @Override
        public void onSearchViewClosed() {

        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_sync_toolbar:
                Util.mostrarToast(getResources().getString(R.string.text_list_atualizar_itens), mContext, Toast.LENGTH_SHORT);
                onResume();
                break;
        }
        return true;
    }

    public void buscarTexto(CharSequence text) {

        if (mRegistroAdapter != null){
            mRegistroAdapter.searchWords(text);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        carregarDados();

        if(getView() == null){
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){
                    // handle back button's click listener

                    Fragment fragment = null;
                    fragment = new ItensFragment();

                    if (fragment != null) {
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        fragmentManager.beginTransaction()
                                .replace(R.id.main_content, fragment)
                                .commit();
                    }

                    mToolbar.setTitle(getResources().getString(R.string.title_fragment_list_itens));
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int REQUEST_CODE_SPEECH_INPUT = 7777;

        if (data != null){
            if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
                if (mToolbarSearch != null) {
                    resultVoice(requestCode, resultCode, data);
                }
            }
        }
    }

    public void resultVoice(int requestCode, int resultCode, Intent data){
        int REQUEST_CODE_SPEECH_INPUT = 7777;
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == getActivity().RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mToolbarSearch.setQuery(result.get(0), false);
            }
        }
    }

}
