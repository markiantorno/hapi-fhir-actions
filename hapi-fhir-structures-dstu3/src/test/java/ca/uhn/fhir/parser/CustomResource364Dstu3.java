package ca.uhn.fhir.parser;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.util.ElementUtil;

/**
 * This is an example of a custom resource that also uses a custom
 * datatype.
 * 
 * See #364
 */
@ResourceDef(name = "CustomResource", profile = "http://hl7.org/fhir/profiles/custom-resource", id = "custom-resource")
public class CustomResource364Dstu3 extends DomainResource {

	private static final long serialVersionUID = 1L;

	@Child(name = "baseValue", min = 1, max = Child.MAX_UNLIMITED, type= {})
	private Type baseValues;

	public Type getBaseValues() {
		return baseValues;
	}


	@Override
	public FhirVersionEnum getStructureFhirVersionEnum() {
		return FhirVersionEnum.DSTU3;
	}

	@Override
	public boolean isEmpty() {
		return ElementUtil.isEmpty(baseValues);
	}

	public void setBaseValues(Type theValue) {
		this.baseValues = theValue;
	}

	@DatatypeDef(name="CustomDate")
	public static class CustomResource364CustomDate extends Type implements ICompositeType {

		private static final long serialVersionUID = 1L;
		
		@Child(name = "date", order = 0, min = 1, max = 1, type = { DateTimeDt.class })
		private DateTimeType date;


		public DateTimeType getDate() {
			if (date == null)
				date = new DateTimeType();
			return date;
		}

		@Override
		public boolean isEmpty() {
			return ElementUtil.isEmpty(date);
		}

		public CustomResource364CustomDate setDate(DateTimeType theValue) {
			date = theValue;
			return this;
		}

		@Override
		protected CustomResource364CustomDate typedCopy() {
			CustomResource364CustomDate retVal = new CustomResource364CustomDate();
			super.copyValues(retVal);
			retVal.date = date;
			return retVal;
		}
	}

	@Override
	public CustomResource364Dstu3 copy() {
		CustomResource364Dstu3 retVal = new CustomResource364Dstu3();
		super.copyValues(retVal);
		retVal.baseValues = baseValues;
		return retVal;
	}


	@Override
	public ResourceType getResourceType() {
		return null;
	}

}