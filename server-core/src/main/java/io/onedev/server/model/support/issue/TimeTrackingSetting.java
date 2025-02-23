package io.onedev.server.model.support.issue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.apache.commons.lang3.StringUtils;

import io.onedev.server.OneDev;
import io.onedev.server.annotation.ChoiceProvider;
import io.onedev.server.annotation.Editable;
import io.onedev.server.entitymanager.LinkSpecManager;
import io.onedev.server.util.usage.Usage;

@Editable
public class TimeTrackingSetting implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final Pattern WORKING_PERIOD_PATTERN = Pattern.compile("(\\d+w)?(\\d+d)?(\\d+h)?(\\d+m)?");
	
	private int hoursPerDay = 8;

	private int daysPerWeek = 5;

	private String aggregationLink;

	@Editable(order=100, description = "Specify working hours per day. This will affect " +
			"parsing and displaying of working periods. For instance <tt>1d</tt> is the " +
			"same as <tt>8h</tt> if this property is set to <tt>8</tt>")
	@Max(24)
	@Min(1)
	public int getHoursPerDay() {
		return hoursPerDay;
	}

	public void setHoursPerDay(int hoursPerDay) {
		this.hoursPerDay = hoursPerDay;
	}

	@Editable(order=200, description = "Specify working days per week. This will affect " +
			"parsing and displaying of working periods. For instance <tt>1w</tt> is the " +
			"same as <tt>5d</tt> if this property is set to <tt>5</tt>")
	@Max(7)
	@Min(1)
	public int getDaysPerWeek() {
		return daysPerWeek;
	}

	public void setDaysPerWeek(int daysPerWeek) {
		this.daysPerWeek = daysPerWeek;
	}

	@Editable(order=500, placeholder = "No aggregation", description = "If specified, total estimated/spent time " +
			"of an issue will also include linked issues of this type")
	@ChoiceProvider("getLinkChoices")
	public String getAggregationLink() {
		return aggregationLink;
	}

	public void setAggregationLink(String aggregationLink) {
		this.aggregationLink = aggregationLink;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getLinkChoices() {
		var choices = new LinkedHashSet<String>();
		for (var linkSpec: OneDev.getInstance(LinkSpecManager.class).query()) {
			if (linkSpec.getOpposite() != null) {
				choices.add(linkSpec.getName());
				choices.add(linkSpec.getOpposite().getName());
			}
		}
		return new ArrayList<>(choices);
	}

	public Usage onDeleteLink(String linkName) {
		Usage usage = new Usage();
		if (linkName.equals(aggregationLink))
			usage.add("time aggregation link");
		return usage;
	}

	public void onRenameLink(String oldName, String newName) {
		if (oldName.equals(aggregationLink))
			aggregationLink = newName;
	}

	public int parseWorkingPeriod(String period) {
		period = StringUtils.deleteWhitespace(period);
		if (StringUtils.isBlank(period))
			throw new ValidationException("Invalid working period");

		if (period.equals("0"))
			return 0;

		Matcher matcher = WORKING_PERIOD_PATTERN.matcher(period);
		if (!matcher.matches())
			throw new ValidationException("Invalid working period");

		int minutes = 0;
		if (matcher.group(1) != null) {
			int weeks = Integer.parseInt(StringUtils.stripEnd(matcher.group(1), "w"));
			minutes += weeks * daysPerWeek * hoursPerDay * 60;
		}

		if (matcher.group(2) != null) {
			int days = Integer.parseInt(StringUtils.stripEnd(matcher.group(2), "d"));
			minutes += days * hoursPerDay * 60;
		}

		if (matcher.group(3) != null) {
			int hours = Integer.parseInt(StringUtils.stripEnd(matcher.group(3), "h"));
			minutes += hours*60;
		}

		if (matcher.group(4) != null)
			minutes += Integer.parseInt(StringUtils.stripEnd(matcher.group(4), "m"));

		return minutes;
	}

	public String formatWorkingPeriod(int minutes) {
		int weeks = minutes / (60 * hoursPerDay * daysPerWeek);
		minutes = minutes % (60 * hoursPerDay * daysPerWeek);
		int days = minutes / (60 * hoursPerDay);
		minutes = minutes % (60 * hoursPerDay);
		int hours = minutes / 60;
		minutes = minutes % 60;

		StringBuilder builder = new StringBuilder();
		if (weeks != 0)
			builder.append(weeks).append("w ");
		if (days != 0)
			builder.append(days).append("d ");
		if (hours != 0)
			builder.append(hours).append("h ");
		if (minutes != 0)
			builder.append(minutes).append("m");

		String formatted = builder.toString().trim();
		if (formatted.length() == 0)
			formatted = "0m";
		return formatted;
	}
	
	public String getWorkingPeriodHelp() {
		return String.format("Expects one or more <tt>&lt;number&gt;(w|d|h|m)</tt>. For instance <tt>1w 1d 1h 1m</tt> represents 1 week (%d days), 1 day (%d hours), 1 hour, and 1 minute", 
				daysPerWeek, hoursPerDay);
	}
	
}
