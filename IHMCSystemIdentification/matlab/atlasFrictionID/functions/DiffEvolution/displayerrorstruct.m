function displayerrorstruct(errorStruct)
%DISPLAYERRORSTRUCT  Display structure returned by function lasterror.
%		DISPLAYERRORSTRUCT displays the structure returned by function
%		LASTERROR. Useful when catching errors.
%
%		Example:
%		try
%		  bla; % fails as 'bla' is probably an unknown function
%		catch
%		  displayerrorstruct(lasterror);
%		end
%
%		Markus Buehren
%		Last modified 21.04.2008
%
%   See also LASTERROR.

if nargin == 0
	errorStruct = lasterror;
end

disp(errorStruct.message);
errorStack = errorStruct.stack;
for k=1:length(errorStack)
	disp(sprintf('Error in ==> %s at %d.', errorStack(k).name, errorStack(k).line));
end
