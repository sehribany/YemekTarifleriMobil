package com.user.yemektarifleriprojesi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_tarif.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.jar.Manifest

class TarifFragment : Fragment() {
    var secilenGorsel:Uri?=null
    var secilenBitmap:Bitmap?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tarif, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button.setOnClickListener {
            kaydet(it)
        }

        imageView2.setOnClickListener {
            gorselsec(it)
        }

        arguments.let {
            var gelenBilgi=TarifFragmentArgs.fromBundle(it).bilgi
            if(gelenBilgi.equals("menudengeldim")){
                //yeni yemek eklemeye geldi
                yemekismiText.setText("")
                yemekmalzemeText.setText("")
                button.visibility=View.VISIBLE

                val gorselsecmeArkaPlani=BitmapFactory.decodeResource(context?.resources,R.drawable.adsiz)
            }else{
                //daha önce oluşturulan yemeğe geldi
                button.visibility=View.INVISIBLE
                val secilenId=TarifFragmentArgs.fromBundle(it).id
                context?.let {
                    try {
                        val db=it.openOrCreateDatabase("Yemekler",Context.MODE_PRIVATE,null)
                        val cursor =db.rawQuery("SELECT * FROM yemekler WHERE id=?", arrayOf(secilenId.toString()))
                        val yemekIsmiIndex=cursor.getColumnIndex("yemekismi")
                        val yemekmalzemeIndex=cursor.getColumnIndex("yemekmalzemesi")
                        val yemekgorseli=cursor.getColumnIndex("gorsel")

                        while(cursor.moveToNext()){
                            yemekismiText.setText(cursor.getString(yemekIsmiIndex))
                            yemekmalzemeText.setText(cursor.getString(yemekmalzemeIndex))

                            val bytedizisi=cursor.getBlob(yemekgorseli)
                            val bitmap=BitmapFactory.decodeByteArray(bytedizisi,0,bytedizisi.size)
                            imageView2.setImageBitmap(bitmap)
                        }

                        cursor.close()
                    }catch (e:Exception){
                        e.printStackTrace()
                    }

                }

            }
        }
    }

    fun kaydet(view:View){ //Sql Kaydet
        val yemekismi=yemekismiText.text.toString()
        val yemekmalzeme=yemekmalzemeText.text.toString()

        if(secilenBitmap != null){
            val kucukBitmap=kucukBitmapOlustur(secilenBitmap !!,300)
            //Bitmapi veriye çevirmek için gerekli olan kodlar
            val outputStream=ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteDizisi=outputStream.toByteArray()

            try {
                context?.let {
                    val database= it.openOrCreateDatabase("Yemekler", Context.MODE_PRIVATE,null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS yemekler (id INTEGER PRIMARY kEY,yemekismi VARCHAR, yemekmalzemesi VARCHAR, gorsel BLOB)")
                    val sqlString= "Insert Into yemekler (yemekismi,yemekmalzemesi,gorsel) VALUES (?,?,?)"
                    val statement=database.compileStatement(sqlString)
                    statement.bindString(1,yemekismi)
                    statement.bindString(2,yemekmalzeme)
                    statement.bindBlob(3,byteDizisi)
                    statement.execute()

                }
            }catch (e:Exception){
                e.printStackTrace()
            }
            val action= TarifFragmentDirections.actionTarifFragmentToListeFragment()
            Navigation.findNavController(view).navigate(action)


        }





    }

    fun gorselsec(view: View){
        //Activity varsa var yoksa yok
        activity?.let {
            if(ContextCompat.checkSelfPermission(it.applicationContext,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //İzin vermediyse,izin iste.
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),1)

            }else{
                //izin verilmiş,izin istemeden galeriye git.
                val galeriintent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriintent,2)
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode==1){
            if(grantResults.size>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //izini aldık.
                val galeriintent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriintent,2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode==2 && resultCode==Activity.RESULT_OK && data !=null){
            secilenGorsel=data.data           //seçilen görselin galerideki yeri
            try {
                context?.let {
                    if(secilenGorsel !=null){

                        if(Build.VERSION.SDK_INT >= 28){
                            val source= ImageDecoder.createSource(it.contentResolver,secilenGorsel!!)
                            secilenBitmap=ImageDecoder.decodeBitmap(source)
                            imageView2.setImageBitmap(secilenBitmap)
                        }else{
                            secilenBitmap=MediaStore.Images.Media.getBitmap(it.contentResolver,secilenGorsel)
                            imageView2.setImageBitmap(secilenBitmap)
                        }

                    }
                }

            }catch (e:Exception){
                e.printStackTrace()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    fun kucukBitmapOlustur(kullanicisectigibitmap:Bitmap,maxboyut:Int):Bitmap{
        var width=kullanicisectigibitmap.width
        var height=kullanicisectigibitmap.height

        val bitmapOran:Double= width.toDouble()/height.toDouble()
        if(bitmapOran>1){ //gorsel yatay

            width=maxboyut
            val kisaltilmisHeight=width/bitmapOran
            height=kisaltilmisHeight.toInt()

        }else{ //gorsel dikey
            height=maxboyut
            val kisaltilmisWidth=height *bitmapOran
            width=kisaltilmisWidth.toInt()

        }

        return Bitmap.createScaledBitmap(kullanicisectigibitmap,width,height,true)
    }

    }
