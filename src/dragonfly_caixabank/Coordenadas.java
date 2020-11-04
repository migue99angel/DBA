
package dragonfly_caixabank;


public class Coordenadas {
    float x;
    float y;
    float z;
    
    Coordenadas(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    Coordenadas(float x, float y)
    {
        this.x = x;
        this.y = y;
        this.z = -1;
    }
    
    Coordenadas()
    {
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }

    @Override
    public String toString() {
        return "Coordenadas{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
    
    public Boolean esIgual(Coordenadas aux){
        Boolean resultado = false;
        if((this.getX() == aux.getX()) && (this.getY() == aux.getY())){
            resultado = true;
            System.out.println("("+this.x+","+this.y+")");
            System.out.println("("+aux.x+","+aux.y+")");
        }

        
        return resultado;
        
    }
    
    
}
