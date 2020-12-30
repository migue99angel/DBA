/***
 * @file DBAMap 
 * @author Luis Castillo, DBA, l.castillo@decsai.ugr.es
 */
package dragonfly_caixabank;

import com.eclipsesource.json.JsonArray;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Luis Castillo Vidal @ DBA
 */
public class DBAMap {
    private BufferedImage _map;
    
    /***
     * Default builder
     */
    public DBAMap()  {
        _map = null;
    }
 
    /***
     * Carga una imagen desde un archivo en una matriz bidimensional de píxeles, cada
     * píxel con un valor RGB tal que R=G=B (escala de grises)
     * @param filename Nombre del fichero
     * @throws IOException Fallos de manejo del fichero
     */
    public void load(String filename) throws IOException  {
        File f;
        
        _map = null;
        f = new File(filename);
        _map = ImageIO.read(f);
    }

    /**
     * Guarda la matriz bidimensional en un fichero cuyo formato viene dado por 
     * la extensión indicada en el nombre del fichero
     * @param filename El fichero a grabar
     * @throws IOException Errores de ficheros
     */
    public void save(String filename) throws IOException  {
        File f;
        
        if (this.hasMap()) {
            f = new File(filename);
            ImageIO.write(_map, "PNG", f);
        }
    }
    
    /**
     * Carga en la matriz bidimensional la imagen que devuelve el SUBSCRIBE de
     * la práctica 3 {"map":[...]}
     * @param map Array JSON que contiene la imagen comprimida para que su envío 
     * entre agentes sea más eficiente
     * @throws IOException Errores de manejo de fiheros temporales
     */
    public void fromJson(JsonArray map) throws IOException  {
        String auxfilename="./tmp/tmp.png";
        byte [] data = new byte[map.size()];
        for (int i=0; i<data.length; i++)
            data[i] = (byte) map.get(i).asInt();
        try {
            FileOutputStream fos = new FileOutputStream(auxfilename);
            fos.write(data);
            fos.close();
            load(auxfilename);
        } catch (Exception ex) {
            System.err.println("*** Error "+ex.toString());
            _map = null;
        }           
    }
    
    /**
     * Devuelve el ancho de la imagen cargada
     * @return Ancho de la imagen
     */
    public int getWidth() {
        if (this.hasMap())
            return _map.getWidth();
        else
            return -1;
    }
    
    /**
     * Devuelve el alto de la imagen cargada
     * @return Alto de la imagen
     */
    public int getHeight() {
        if (this.hasMap())
            return _map.getHeight();
        else
            return -1;
    }

    /**
     * 
     * Devuelve la altura del mapa en las coordenadas especificadas
     * @param x Coordenada del mapa
     * @param y Coordenada del mapa
     * @return Altura del terreno en (x,y)
     */
    public int getLevel(int x, int y) {
          return getPixel(x,y).getBlue();
    }
    
    /**
     * Comprueba que hay una imagen ya cargada
     * @return true si hay una imagen cargada, false en otro caso
     */
    private boolean hasMap() {
        return (_map != null);
    }      

    /**
     * Consulta el valor del pixel del mapa en las coordenadas indicadas
     * @param x Coordenada del mapa
     * @param y Coordenada del mapa
     * @return El color (grises) del mapa en ese punto
     */
    private Color getPixel(int x, int y) {
        if (this.hasMap() && 0 <= x && x < this.getWidth() && 0 <= y && y < this.getHeight())  {
            return new Color(_map.getRGB(x, y)); 
        }else  {
            return Color.BLACK;
        }
    }
    
}
