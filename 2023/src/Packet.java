import java.io.Serializable;

public class Packet implements Serializable{

    public enum Type{
        TEXT,
        VIDEO,
        AUDIO
    }

    private Type tipo;
    private String conteudo;

    public Packet(Type tipo, String conteudo){
        this.tipo=tipo;
        switch(this.tipo){
            case TEXT:
                this.tipo = tipo;
                this.conteudo = conteudo;
                break;
            //Fazer o packet para video e para audio
        }
    }

    public String getConteudo(){
        return this.conteudo;
    }

    public Type getTipo(){
        return this.tipo;
    }

}