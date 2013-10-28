
package dataBase;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import dataBase.*;

/**
 *
 * @author Joao Machete
 */
@Entity
@Table(name = "ZONE_DEFINITIONS", catalog = "", schema = "APP")
@NamedQueries({@NamedQuery(name = "ZoneDefinitions.findAll", query = "SELECT z FROM ZoneDefinitions z"), @NamedQuery(name = "ZoneDefinitions.findById", query = "SELECT z FROM ZoneDefinitions z WHERE z.id = :id"), @NamedQuery(name = "ZoneDefinitions.findByZoneDefName", query = "SELECT z FROM ZoneDefinitions z WHERE z.zoneDefName = :zoneDefName"), @NamedQuery(name = "ZoneDefinitions.findByUpperleftx", query = "SELECT z FROM ZoneDefinitions z WHERE z.upperleftx = :upperleftx"), @NamedQuery(name = "ZoneDefinitions.findByUpperlefty", query = "SELECT z FROM ZoneDefinitions z WHERE z.upperlefty = :upperlefty"), @NamedQuery(name = "ZoneDefinitions.findByZoneWith", query = "SELECT z FROM ZoneDefinitions z WHERE z.zoneWith = :zoneWith"), @NamedQuery(name = "ZoneDefinitions.findByZoneHeight", query = "SELECT z FROM ZoneDefinitions z WHERE z.zoneHeight = :zoneHeight"), @NamedQuery(name = "ZoneDefinitions.findByUppercase", query = "SELECT z FROM ZoneDefinitions z WHERE z.uppercase = :uppercase"), @NamedQuery(name = "ZoneDefinitions.findByLowercase", query = "SELECT z FROM ZoneDefinitions z WHERE z.lowercase = :lowercase"), @NamedQuery(name = "ZoneDefinitions.findByNumberType", query = "SELECT z FROM ZoneDefinitions z WHERE z.numberType = :numberType"), @NamedQuery(name = "ZoneDefinitions.findByTitlecase", query = "SELECT z FROM ZoneDefinitions z WHERE z.titlecase = :titlecase")})
public class ZoneDefinitions implements Serializable {
    @Transient
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID", nullable = false)
    private Integer id;
    @Column(name = "ZONE_DEF_NAME", length = 40)
    private String zoneDefName;
    @Column(name = "UPPERLEFTX")
    private Integer upperleftx;
    @Column(name = "UPPERLEFTY")
    private Integer upperlefty;
    @Column(name = "ZONE_WITH")
    private Integer zoneWith;
    @Column(name = "ZONE_HEIGHT")
    private Integer zoneHeight;
    @Column(name = "UPPERCASE")
    private Character uppercase;
    @Column(name = "LOWERCASE")
    private Character lowercase;
    @Column(name = "NUMBER_TYPE")
    private Character numberType;
    @Column(name = "TITLECASE")
    private Character titlecase;
    @JoinColumn(name = "ZONE_DEFINITIONS_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(optional = false)
    private Zonetype zoneDefinitionsId;

    public ZoneDefinitions() {
    }

    public ZoneDefinitions(Integer id) {
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

    public String getZoneDefName() {
        return zoneDefName;
    }

    public void setZoneDefName(String zoneDefName) {
        String oldZoneDefName = this.zoneDefName;
        this.zoneDefName = zoneDefName;
        changeSupport.firePropertyChange("zoneDefName", oldZoneDefName, zoneDefName);
    }

    public Integer getUpperleftx() {
        return upperleftx;
    }

    public void setUpperleftx(Integer upperleftx) {
        Integer oldUpperleftx = this.upperleftx;
        this.upperleftx = upperleftx;
        changeSupport.firePropertyChange("upperleftx", oldUpperleftx, upperleftx);
    }

    public Integer getUpperlefty() {
        return upperlefty;
    }

    public void setUpperlefty(Integer upperlefty) {
        Integer oldUpperlefty = this.upperlefty;
        this.upperlefty = upperlefty;
        changeSupport.firePropertyChange("upperlefty", oldUpperlefty, upperlefty);
    }

    public Integer getZoneWith() {
        return zoneWith;
    }

    public void setZoneWith(Integer zoneWith) {
        Integer oldZoneWith = this.zoneWith;
        this.zoneWith = zoneWith;
        changeSupport.firePropertyChange("zoneWith", oldZoneWith, zoneWith);
    }

    public Integer getZoneHeight() {
        return zoneHeight;
    }

    public void setZoneHeight(Integer zoneHeight) {
        Integer oldZoneHeight = this.zoneHeight;
        this.zoneHeight = zoneHeight;
        changeSupport.firePropertyChange("zoneHeight", oldZoneHeight, zoneHeight);
    }

    public Character getUppercase() {
        return uppercase;
    }

    public void setUppercase(Character uppercase) {
        Character oldUppercase = this.uppercase;
        this.uppercase = uppercase;
        changeSupport.firePropertyChange("uppercase", oldUppercase, uppercase);
    }

    public Character getLowercase() {
        return lowercase;
    }

    public void setLowercase(Character lowercase) {
        Character oldLowercase = this.lowercase;
        this.lowercase = lowercase;
        changeSupport.firePropertyChange("lowercase", oldLowercase, lowercase);
    }

    public Character getNumberType() {
        return numberType;
    }

    public void setNumberType(Character numberType) {
        Character oldNumberType = this.numberType;
        this.numberType = numberType;
        changeSupport.firePropertyChange("numberType", oldNumberType, numberType);
    }

    public Character getTitlecase() {
        return titlecase;
    }

    public void setTitlecase(Character titlecase) {
        Character oldTitlecase = this.titlecase;
        this.titlecase = titlecase;
        changeSupport.firePropertyChange("titlecase", oldTitlecase, titlecase);
    }

    public Zonetype getZoneDefinitionsId() {
        return zoneDefinitionsId;
    }

    public void setZoneDefinitionsId(Zonetype zoneDefinitionsId) {
        Zonetype oldZoneDefinitionsId = this.zoneDefinitionsId;
        this.zoneDefinitionsId = zoneDefinitionsId;
        changeSupport.firePropertyChange("zoneDefinitionsId", oldZoneDefinitionsId, zoneDefinitionsId);
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
        if (!(object instanceof ZoneDefinitions)) {
            return false;
        }
        ZoneDefinitions other = (ZoneDefinitions) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "dataBase.ZoneDefinitions[id=" + id + "]";
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

}
