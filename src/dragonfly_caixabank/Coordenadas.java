package dragonfly_caixabank;

/**
 * Clase auxiliar para el manejo de las Coordenadas
 * @version 1.0
 * @author Francisco Domínguez Lorente
 * @author José María Gómez García
 * @author Miguel Muñoz Molina
 * @author Miguel Ángel Posadas Arráez
 */
public class Coordenadas {
    float x;
    float y;
    float z;
    
    /**
     * Constructor por parámetros
     * @param x Coordenada x
     * @param y Coordenada y
     * @param z Coordenada z
     */
    Coordenadas(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * Constructor por parámetros
     * @param x Coordenada x
     * @param y Coordenada y
     */
    Coordenadas(float x, float y)
    {
        this.x = x;
        this.y = y;
        this.z = -1;
    }
    
    /**
     * Constructor por defecto
     */
    Coordenadas()
    {
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }
    
    /**
     * Getter del atributo x
     * @return Coordenada x
     */
    public float getX() {
        return x;
    }

    /**
     * Getter del atributo y
     * @return Coordenada y
     */
    public float getY() {
        return y;
    }

    /**
     * Getter del atributo z
     * @return Coordenada z
     */
    public float getZ() {
        return z;
    }

    /**
     * Setter del atributo x
     * @param x 
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * Setter del atributo y
     * @param y 
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * Setter del atributo z
     * @param z 
     */
    public void setZ(float z) {
        this.z = z;
    }

    /**
     * Método toString
     * @return String conteniendo los valores de los tres atributos de la clase
     */
    @Override
    public String toString() {
        return "Coordenadas{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
    
    /**
     * Método para comprobar si los componentes x,y de un objeto Coordenadas
     * coinciden con los componentes del objeto Coordenadas actual
     * @param aux Objeto Coordenadas a comprobar
     * @return True si coinciden, false en caso contrario
     */
    public Boolean esIgual(Coordenadas aux){
        Boolean resultado = false;
        
        if((this.getX() == aux.getX()) && (this.getY() == aux.getY())) {
            resultado = true;
        }
        
        return resultado;
        
    }   
}
