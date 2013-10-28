
package dataBase;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import dataBase.*;

/**
 *
 * @author Joao Machete
 */
@Entity
@Table(name = "ZONETYPE", catalog = "", schema = "APP")
@NamedQueries({@NamedQuery(name = "Zonetype.findAll", query = "SELECT z FROM Zonetype z"), @NamedQuery(name = "Zonetype.findById", query = "SELECT z FROM Zonetype z WHERE z.id = :id"), @NamedQuery(name = "Zonetype.findByZoneTypeName", query = "SELECT z FROM Zonetype z WHERE z.zoneTypeName = :zoneTypeName")})
public class Zonetype implements Serializable {
    @Transient
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID", nullable = false)
    private Integer id;
    @Column(name = "ZONE_TYPE_NAME", length = 40)
    private String zoneTypeName;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "zoneDefinitionsId")
    private List<ZoneDefinitions> zoneDefinitionsList;

    public Zonetype() {
    }

    public Zonetype(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        Integer oldId = this.id;
        this.id = id;
        changeSupport.firePropertyChange("id", oldId, id);
    }

    public String getZoneTypeName() {
        return zoneTypeName;
    }

    public void setZoneTypeName(String zoneTypeName) {
        String oldZoneTypeName = this.zoneTypeName;
        this.zoneTypeName = zoneTypeName;
        changeSupport.firePropertyChange("zoneTypeName", oldZoneTypeName, zoneTypeName);
    }

    public List<ZoneDefinitions> getZoneDefinitionsList() {
        return zoneDefinitionsList;
    }

    public void setZoneDefinitionsList(List<ZoneDefinitions> zoneDefinitionsList) {
        this.zoneDefinitionsList = zoneDefinitionsList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Zonetype)) {
            return false;
        }
        Zonetype other = (Zonetype) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "dataBase.Zonetype[id=" + id + "]";
//        return "enaocr.Zonetype[zoneTypeName=" + zoneTypeName + "]";
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

}
